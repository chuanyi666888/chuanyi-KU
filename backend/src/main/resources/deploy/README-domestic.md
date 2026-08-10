# =====================================================
# 智慧粮库 - 国产化适配部署指南
# =====================================================
#
# 适用：Phase 7 - 国产化适配
# 目标平台：
#   - 操作系统：麒麟 V11 SP3 (aarch64 / x86_64)
#   - 数据库：KingbaseES V8R6（替代 MySQL）
#   - 缓存：Redis 7.x
#   - JDK：毕昇 JDK 17 / OpenJDK 17
#   - Web 服务器：Nginx 1.20+
#   - 容器：Docker 20.10+（可选）
# =====================================================

## 1. 软件清单

| 类别 | 名称 | 版本 | 备注 |
|------|------|------|------|
| OS | 麒麟 V11 SP3 | 11.x | 国产化操作系统 |
| JDK | 毕昇 JDK 17 | 17.0.x | 华为开源，aarch64 优化 |
| 数据库 | KingbaseES | V8R6 | 替代 MySQL 8 |
| 缓存 | Redis | 7.x | 直接使用 |
| Web | Nginx | 1.20+ | 静态资源 + 反代 |
| 监控 | Prometheus | 2.51+ | 见 Phase 5 |
| 监控 | Grafana | 10.x | 见 Phase 5 |

## 2. 麒麟 V11 系统准备

### 2.1 安装毕昇 JDK 17
```bash
# 下载（华为开源镜像站）
wget https://mirrors.huaweicloud.com/kunpeng/archive/compiler/bisheng_jdk/bisheng-jdk-17.0.10-linux-x64.tar.gz

# 或 aarch64
# wget https://mirrors.huaweicloud.com/kunpeng/archive/compiler/bisheng_jdk/bisheng-jdk-17.0.10-linux-aarch64.tar.gz

sudo mkdir -p /usr/local/java
sudo tar -xzf bisheng-jdk-17.0.10-linux-*.tar.gz -C /usr/local/java --strip-components=1

# 配置环境变量
sudo tee /etc/profile.d/java.sh > /dev/null <<'EOF'
export JAVA_HOME=/usr/local/java
export PATH=$JAVA_HOME/bin:$PATH
export JAVA_OPTS="-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
EOF
source /etc/profile.d/java.sh

# 验证
java -version
# openjdk version "17.0.10" 2024-01-16
# OpenJDK Runtime Environment Bisheng JDK 17.0.10 (build 17.0.10+10-LTS)
```

### 2.2 创建系统用户
```bash
sudo useradd --no-create-home --shell /sbin/nologin smartgrain
sudo mkdir -p /opt/smartgrain /var/log/smartgrain
sudo chown smartgrain:smartgrain /opt/smartgrain /var/log/smartgrain
```

### 2.3 防火墙
```bash
sudo firewall-cmd --permanent --add-port=8090/tcp   # 后端
sudo firewall-cmd --permanent --add-port=443/tcp    # Nginx
sudo firewall-cmd --permanent --add-port=9090/tcp   # Prometheus（可选，仅内网）
sudo firewall-cmd --reload
```

## 3. KingbaseES 数据库部署

### 3.1 安装 KingbaseES V8R6
```bash
# 解压安装包（从金仓官网获取）
unzip KingbaseES_V008R006C008B0014_Lin64.tar.gz
cd KingbaseES_V008R006C008B0014_Lin64

# 安装到 /opt/kingbase
sudo mkdir -p /opt/kingbase
sudo chown smartgrain:smartgrain /opt/kingbase
./setup.sh -i silent -f response-file

# 配置环境变量
sudo tee /etc/profile.d/kingbase.sh > /dev/null <<'EOF'
export KINGBASE_HOME=/opt/kingbase/Server
export PATH=$KINGBASE_HOME/bin:$PATH
EOF
```

### 3.2 初始化数据库
```bash
# 初始化数据目录
sudo -u smartgrain initdb -D /opt/kingbase/data -U smartgrain --encoding=UTF8 --locale=zh_CN.UTF-8

# 启动
sudo -u smartgrain pg_ctl -D /opt/kingbase/data -l /var/log/smartgrain/kingbase.log start

# 创建业务库
ksql -U smartgrain -d postgres -c "CREATE DATABASE smart_grain ENCODING 'UTF8';"
```

### 3.3 应用迁移脚本
```bash
# KingbaseES 兼容 PostgreSQL 语法，需要将 MySQL DDL 做最小化适配：
# - 替换 ` 反引号为 "
# - 替换 ENGINE=InnoDB AUTO_INCREMENT 为 PostgreSQL 风格
# - 替换 DATETIME 为 TIMESTAMP
# - 替换 TINYINT 为 SMALLINT
# - 替换 ON UPDATE CURRENT_TIMESTAMP 为触发器
# - 替换 BIGINT NOT NULL 主键为 BIGSERIAL

# 项目已提供 KingbaseES 适配脚本（如有）：
# ksql -U smartgrain -d smart_grain -f db/migration/kingbase/V1__init_schema.sql

# MySQL 转 KingbaseES 可使用 KCSQL 转换工具：
# https://help.kingbase.com.cn/
```

### 3.4 添加 JDBC 驱动
```xml
<!-- pom.xml 添加 KingbaseES 驱动 -->
<dependency>
    <groupId>cn.com.kingbase</groupId>
    <artifactId>kingbase8</artifactId>
    <version>8.6.0</version>
</dependency>
```

### 3.5 使用 KingbaseES 配置启动
```bash
# 复制 KingbaseES 专用配置
cp backend/src/main/resources/deploy/application-kingbase.yml /opt/smartgrain/

# 启动时指定 profile
java -jar smart-grain-backend.jar \
    --spring.profiles.active=prod,kingbase \
    --spring.config.additional-location=/opt/smartgrain/
```

## 4. 应用部署

### 4.1 复制文件
```bash
sudo cp backend/target/smart-grain-backend.jar /opt/smartgrain/
sudo cp -r frontend/dist/* /opt/smartgrain/frontend/
sudo cp backend/src/main/resources/deploy/application-prod.yml /opt/smartgrain/

# systemd 服务
sudo cp backend/src/main/resources/deploy/smart-grain-backend.service /etc/systemd/system/
sudo cp backend/src/main/resources/deploy/smart-grain-nginx.service /etc/systemd/system/

# Nginx 配置
sudo cp backend/src/main/resources/deploy/nginx-smart-grain.conf /etc/nginx/conf.d/
```

### 4.2 配置环境变量
```bash
sudo tee /opt/smartgrain/smartgrain.env > /dev/null <<'EOF'
DB_HOST=127.0.0.1
DB_PORT=54321
DB_NAME=smart_grain
DB_USERNAME=smartgrain
DB_PASSWORD=<your-password>
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
JWT_SECRET=<your-secret-at-least-32-bytes>
SPRING_PROFILES_ACTIVE=prod,kingbase
EOF
sudo chmod 600 /opt/smartgrain/smartgrain.env
sudo chown smartgrain:smartgrain /opt/smartgrain/smartgrain.env
```

### 4.3 启动 systemd 服务
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now smart-grain-backend
sudo systemctl enable --now smart-grain-nginx

# 查看状态
sudo systemctl status smart-grain-backend
sudo journalctl -u smart-grain-backend -f
```

### 4.4 验证
```bash
# 健康检查
curl http://localhost:8090/actuator/health
# {"status":"UP"}

# 通过 Nginx 访问
curl -k https://localhost/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@123456"}'
```

## 5. MySQL → KingbaseES SQL 适配要点

| MySQL | KingbaseES | 说明 |
|-------|-----------|------|
| `BIGINT NOT NULL` 主键 | `BIGSERIAL` 或应用层 ASSIGN_ID | 雪花算法在应用层无影响 |
| `VARCHAR(n)` | `VARCHAR(n)` | 一致 |
| `DATETIME` | `TIMESTAMP` | 时区处理一致 |
| `TINYINT` | `SMALLINT` | KingbaseES 无 TINYINT |
| `DECIMAL(10,2)` | `DECIMAL(10,2)` | 一致 |
| `ENGINE=InnoDB ...` | 删除 | KingbaseES 无存储引擎概念 |
| `ON UPDATE CURRENT_TIMESTAMP` | 触发器 | 见下方示例 |
| 反引号 `` ` `` | 双引号 `"` | KingbaseES 用双引号 |
| `INSERT ... ON DUPLICATE KEY UPDATE` | `INSERT ... ON CONFLICT DO UPDATE` | UPSERT 语法 |
| `LIMIT n, m` | `LIMIT m OFFSET n` | 分页语法 |

### ON UPDATE CURRENT_TIMESTAMP 触发器示例
```sql
-- 替代：updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_alert_record_updated
    BEFORE UPDATE ON alert_record
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
```

## 6. 离线安装包制作

生产环境通常无外网，需提前制作离线安装包：

```bash
# 在有网的机器上准备
mkdir -p offline-pkg/{java,kingbase,redis,nginx,app}

# JDK
cp bisheng-jdk-17.0.10-linux-*.tar.gz offline-pkg/java/

# KingbaseES
cp KingbaseES_V008R006C008B0014_Lin64.tar.gz offline-pkg/kingbase/

# Redis（麒麟 V11 自带或下载 RPM）
yumdownloader --resolve redis
mv redis-*.rpm offline-pkg/redis/

# Nginx
yumdownloader --resolve nginx
mv nginx-*.rpm offline-pkg/nginx/

# 应用
cp backend/target/smart-grain-backend.jar offline-pkg/app/
cp -r frontend/dist offline-pkg/app/frontend

# Maven 依赖（含 KingbaseES 驱动）已打入 jar，无需单独打包

# 打包
tar czf smart-grain-offline.tar.gz offline-pkg/
```

## 7. 安全加固清单

- [ ] KingbaseES 修改默认端口 54321
- [ ] KingbaseES 配置 pg_hba.conf 仅允许本机连接
- [ ] Redis 开启密码认证，绑定 127.0.0.1
- [ ] Nginx 启用 HTTPS，使用国密 SM2 证书（可选）
- [ ] JWT secret 长度 ≥ 32 字节，使用随机字符串
- [ ] admin 默认密码首次登录后修改
- [ ] /actuator/prometheus 加 IP 白名单
- [ ] 防火墙仅开放 443
- [ ] 定期备份（见 Phase 8）
- [ ] 日志轮转配置（logrotate）

## 8. 日志轮转
```bash
sudo tee /etc/logrotate.d/smartgrain > /dev/null <<'EOF'
/var/log/smartgrain/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0640 smartgrain smartgrain
    sharedscripts
    postrotate
        systemctl reload smart-grain-backend > /dev/null 2>&1 || true
    endscript
}
EOF
```

## 9. 性能调优（aarch64 / 鲲鹏）

```bash
# JVM 参数（鲲鹏 920 64核建议）
JAVA_OPTS="-Xms4g -Xmx4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+ParallelRefProcEnabled \
    -XX:+UseStringDeduplication \
    -XX:+UnlockDiagnosticVMOptions \
    -XX:+UseNUMA \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai"
```
