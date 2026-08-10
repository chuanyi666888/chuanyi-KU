# 智慧粮库 - 备份恢复系统（Phase 8）

## 架构总览

```
┌──────────────────────────────────────────────────────────────┐
│                       生产服务器 (主)                          │
│  ┌────────────┐   ┌────────────┐   ┌────────────────────┐   │
│  │ MySQL 8    │   │ KingbaseES │   │ 应用上传文件        │   │
│  └─────┬──────┘   └─────┬──────┘   └─────────┬──────────┘   │
│        │                │                    │              │
│        ▼                ▼                    ▼              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           备份脚本 (cron 定时触发)                    │  │
│  │  mysql-backup.sh / kingbase-backup.sh / rsync-sync.sh │  │
│  └─────────────────────────┬────────────────────────────┘  │
│                            │                                │
│              ┌─────────────┼─────────────┐                 │
│              ▼             ▼             ▼                 │
│         /data/backups  /data/backups  /data/backups        │
│           /mysql         /kingbase     /app-uploads         │
└──────────────────────────┬───────────────────────────────────┘
                           │ rsync over SSH (异地容灾)
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                    异地备份服务器 (备)                         │
│              /data/backups/{mysql,kingbase,app-uploads}      │
└──────────────────────────────────────────────────────────────┘

                           ▲ 恢复演练（每月 1 日）
                           │
┌──────────────────────────────────────────────────────────────┐
│                  隔离演练环境 (DRILL_DB_PORT=3307)             │
│   restore-drill.sh 自动恢复 + 完整性校验 + 业务抽样核对       │
└──────────────────────────────────────────────────────────────┘
```

## 文件清单

| 文件 | 用途 |
|------|------|
| `mysql-backup.sh` | MySQL 全量+增量备份、自动清理、完整性校验 |
| `kingbase-backup.sh` | KingbaseES 逻辑+物理备份、WAL 归档、清理 |
| `rsync-sync.sh` | 异地备份服务器同步（SSH 免密） |
| `restore-drill.sh` | 灾备恢复演练（隔离环境+完整性校验） |
| `cron-smart-grain-backup` | cron 定时任务配置示例 |

后端 API：
- `BackupController` - 备份记录查询、完整性校验、手动触发
- `BackupService` - 备份业务逻辑
- 权限：`system:backup:view`（查询）、`system:backup:manage`（触发）

## 部署步骤

### 1. 准备目录与权限

```bash
# 创建备份目录
sudo mkdir -p /data/backups/{mysql,kingbase}
sudo mkdir -p /opt/smartgrain/scripts/backup
sudo mkdir -p /var/log/smartgrain

# 创建运行用户
sudo useradd -r -m -d /opt/smartgrain -s /bin/bash smartgrain

# 复制脚本
sudo cp mysql-backup.sh kingbase-backup.sh rsync-sync.sh restore-drill.sh \
    /opt/smartgrain/scripts/backup/
sudo chmod +x /opt/smartgrain/scripts/backup/*.sh
sudo chown -R smartgrain:smartgrain /opt/smartgrain/scripts /data/backups /var/log/smartgrain
```

### 2. 配置环境变量

创建 `/etc/profile.d/smartgrain-backup.sh`：

```bash
# MySQL
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD='你的MySQL密码'
export DB_NAME=smart_grain

# KingbaseES（如使用）
export KB_HOST=127.0.0.1
export KB_PORT=54321
export KB_USER=smartgrain
export KB_PASSWORD='KingbaseES密码'
export KB_DB=smart_grain
export KB_HOME=/opt/kingbase8

# 备份根目录
export BACKUP_ROOT=/data/backups/mysql

# rsync 远程服务器
export REMOTE_USER=backup
export REMOTE_HOST=backup.example.com
export REMOTE_PORT=22
export SSH_KEY=/home/smartgrain/.ssh/id_rsa_backup

# 演练数据库（独立实例，端口 3307）
export DRILL_DB_HOST=127.0.0.1
export DRILL_DB_PORT=3307
export DRILL_DB_USER=root
export DRILL_DB_PASSWORD='drill_password'
```

加载配置：
```bash
sudo chmod 600 /etc/profile.d/smartgrain-backup.sh
source /etc/profile.d/smartgrain-backup.sh
```

### 3. 配置 SSH 免密登录（异地同步）

```bash
# 切换到 smartgrain 用户
su - smartgrain

# 生成密钥
ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa_backup -N ""

# 复制公钥到远程备份服务器
ssh-copy-id -i ~/.ssh/id_rsa_backup.pub -p 22 backup@backup.example.com

# 测试
ssh -i ~/.ssh/id_rsa_backup backup@backup.example.com "echo ok"
```

### 4. 安装 cron 定时任务

```bash
sudo cp cron-smart-grain-backup /etc/cron.d/
sudo chmod 644 /etc/cron.d/smart-grain-backup
sudo systemctl reload cron
```

### 5. MySQL binlog 配置（增量备份必需）

`/etc/my.cnf` 或 `/etc/mysql/conf.d/binlog.cnf`：

```ini
[mysqld]
server-id = 1
log_bin = /var/lib/mysql/mysql-bin
binlog_format = ROW
expire_logs_days = 7
binlog_row_image = MINIMAL
```

重启 MySQL 后生效。

### 6. KingbaseES WAL 归档配置

`kingbase.conf`：

```conf
wal_level = replica
archive_mode = on
archive_command = 'test ! -f /opt/kingbase8/data/archive_wal/%f && cp %p /opt/kingbase8/data/archive_wal/%f'
```

### 7. 执行数据库迁移

```bash
mysql -uroot -p smart_grain < backend/src/main/resources/db/migration/mysql/V5__backup_system.sql
```

### 8. 重启后端服务

```bash
sudo systemctl restart smart-grain-backend
```

## 使用方式

### 手动执行备份

```bash
# MySQL 全量备份
/opt/smartgrain/scripts/backup/mysql-backup.sh full

# MySQL 增量备份
/opt/smartgrain/scripts/backup/mysql-backup.sh incremental

# 完整流程（备份+增量+清理+校验）
/opt/smartgrain/scripts/backup/mysql-backup.sh all
```

### 手动恢复

```bash
# 解压并恢复
zcat /data/backups/mysql/daily/20260728/smart_grain_full_20260728.sql.gz | \
    mysql -uroot -p smart_grain

# KingbaseES 恢复
/opt/kingbase8/bin/sys_restore -h 127.0.0.1 -p 54321 -U smartgrain \
    -d smart_grain -v /data/backups/kingbase/daily/20260728/smart_grain_full_20260728.dmp
```

### 按时间点恢复（PITR）

```bash
# 1. 恢复最近的全量备份
zcat /data/backups/mysql/daily/20260728/smart_grain_full_20260728.sql.gz | mysql -uroot -p smart_grain

# 2. 应用 binlog 到指定时间点
mysqlbinlog --start-datetime="2026-07-28 02:00:00" \
            --stop-datetime="2026-07-28 10:30:00" \
            /data/backups/mysql/binlog/20260728/mysql-bin.0000* | mysql -uroot -p
```

### 恢复演练

```bash
# 演练 MySQL 今日备份
/opt/smartgrain/scripts/backup/restore-drill.sh mysql

# 演练指定日期
/opt/smartgrain/scripts/backup/restore-drill.sh mysql 20260728

# 演练 KingbaseES
/opt/smartgrain/scripts/backup/restore-drill.sh kingbase
```

演练报告输出到 `/var/log/smartgrain/drill/`。

### 通过 API 管理

```bash
# 查看备份统计
curl -H "Authorization: Bearer <token>" http://localhost:8090/api/v1/backup/stats

# 列出今日备份
curl -H "Authorization: Bearer <token>" http://localhost:8090/api/v1/backup/today

# 校验某备份
curl -H "Authorization: Bearer <token>" \
    "http://localhost:8090/api/v1/backup/verify?dbKind=mysql&category=daily&backupDate=20260728"

# 手动触发备份（异步）
curl -X POST -H "Authorization: Bearer <token>" \
    "http://localhost:8090/api/v1/backup/trigger?dbKind=mysql&action=full"

# 查询任务状态
curl -H "Authorization: Bearer <token>" \
    http://localhost:8090/api/v1/backup/tasks/bk_1234567890
```

## 备份保留策略

| 分类 | 频率 | 保留时间 | 文件类型 |
|------|------|---------|---------|
| daily | 每日 | 7 天 | sql.gz / dmp |
| weekly | 每周日 | 4 周 | sql.gz / dmp |
| monthly | 每月 1 日 | 6 个月 | sql.gz / dmp |
| binlog | 每 6 小时 | 3 天 | mysql-bin.NNNNNN |
| wal | 每 6 小时 | 3 天 | WAL 段文件 |
| base | 每周一 | 4 周 | tar.gz 物理备份 |

## 完整性校验机制

每个备份目录包含 `META.txt`，记录：
- `backup_type`: 备份类型（full/incremental）
- `backup_file`: 文件名
- `backup_size`: 文件大小（字节）
- `backup_md5`: MD5 校验和
- `backup_time`: 备份时间
- `database` / `host`: 数据库与主机信息

校验流程：
1. **MD5 校验** - 重算 MD5 与 META 中记录比对
2. **gzip 完整性** - `gzip -t` 测试压缩文件完整性
3. **SQL 头部校验** - 检查文件头是否为 `-- MySQL dump`
4. **业务数据抽样** - 演练脚本会验证关键表（sys_user/grain_batch/inventory 等）可读性
5. **业务规则校验** - 如 inventory.current_tons 不能为负

## 故障恢复 RTO/RPO 目标

| 场景 | RPO | RTO | 恢复方式 |
|------|-----|-----|---------|
| 单表误删 | 6 小时 | 30 分钟 | 全量+binlog 重放到误删前 |
| 整库灾难 | 6 小时 | 2 小时 | 全量+异地同步恢复 |
| 服务器故障 | 6 小时 | 4 小时 | 异地备份服务器接管 |

## 监控建议

1. **备份成功率监控**：在 cron 日志中搜索 `[ERR]` 关键字
2. **备份大小监控**：通过 Prometheus + BusinessMetrics 监控备份文件大小变化
3. **演练结果告警**：每月 1 日演练失败时发送告警
4. **磁盘空间监控**：备份目录磁盘使用率 > 80% 时告警

```bash
# 检查今日备份是否成功
grep "$(date '+%F')" /var/log/smartgrain/cron-backup.log | grep -E '\[OK\]|\[ERR\]'
```

## 安全注意事项

1. **密码保护**：环境变量配置文件 `/etc/profile.d/smartgrain-backup.sh` 必须 `chmod 600`
2. **备份加密**：异地传输建议使用 SSH 隧道，密钥单独管理
3. **权限隔离**：演练数据库使用独立端口（3307/54322），不与生产混用
4. **审计追踪**：所有 API 触发的备份操作通过 `AuditLogAspect` 记录审计日志
5. **最小权限**：备份用户仅授予 SELECT/LOCK TABLES/RELOAD/REPLICATION CLIENT 权限

```sql
-- 创建专用备份账号（推荐）
CREATE USER 'backup'@'localhost' IDENTIFIED BY '强密码';
GRANT SELECT, LOCK TABLES, RELOAD, REPLICATION CLIENT, SHOW VIEW, EVENT, TRIGGER 
    ON smart_grain.* TO 'backup'@'localhost';
FLUSH PRIVILEGES;
```
