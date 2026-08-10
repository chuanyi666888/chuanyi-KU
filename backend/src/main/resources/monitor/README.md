# =====================================================
# 智慧粮库 - 监控集成部署指南
# =====================================================
#
# 适用：Phase 5 - Prometheus + Grafana + Actuator 集成
# 平台：Linux（麒麟 V11 / Ubuntu 22.04 已验证）
# =====================================================

## 1. 架构概览

```
[Spring Boot 8090] --/actuator/prometheus--> [Prometheus 9090] --查询--> [Grafana 3000]
        |                                                                 |
        +-- 业务指标 (smartgrain_*)                                       +-- 告警仪表盘
        +-- JVM 指标 (jvm_*)
        +-- HTTP 指标 (http_server_requests_*)
```

## 2. 后端配置（已完成）

### 2.1 依赖（pom.xml）
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`

### 2.2 application.yml（已配置）
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: ${spring.application.name}
```

### 2.3 自定义业务指标（BusinessMetrics.java）
- `smartgrain_stock_tons_total`           库存总量（吨）
- `smartgrain_alerts_total{status=...}`   各状态告警数
- `smartgrain_sensors_total{status=...}`  在线/离线传感器数

### 2.4 安全配置（SecurityConstants.java）
`/actuator/health` `/actuator/info` `/actuator/prometheus` 已加入 PUBLIC_PATHS
（生产环境建议改为仅内网可访问，或加 Basic Auth）

## 3. Prometheus 部署

### 3.1 安装（Ubuntu/麒麟）
```bash
# 创建用户
sudo useradd --no-create-home --shell /bin/false prometheus

# 创建目录
sudo mkdir -p /etc/prometheus /var/lib/prometheus
sudo chown prometheus:prometheus /var/lib/prometheus

# 下载并解压（替换为实际版本号）
cd /tmp
wget https://github.com/prometheus/prometheus/releases/download/v2.51.0/prometheus-2.51.0.linux-amd64.tar.gz
tar xvfz prometheus-*.tar.gz
cd prometheus-*/

# 复制二进制
sudo cp prometheus /usr/local/bin/
sudo cp promtool /usr/local/bin/

# 复制配置
sudo cp -r consoles /etc/prometheus/
sudo cp -r console_libraries /etc/prometheus/
```

### 3.2 配置
```bash
# 复制本项目提供的配置
sudo cp backend/src/main/resources/monitor/prometheus.yml /etc/prometheus/prometheus.yml
sudo cp backend/src/main/resources/monitor/alerts.yml /etc/prometheus/rules/smart-grain.yml
sudo chown -R prometheus:prometheus /etc/prometheus
```

### 3.3 systemd 服务
```bash
sudo tee /etc/systemd/system/prometheus.service > /dev/null <<'EOF'
[Unit]
Description=Prometheus
Wants=network-online.target
After=network-online.target

[Service]
User=prometheus
Group=prometheus
Type=simple
ExecStart=/usr/local/bin/prometheus \
    --config.file /etc/prometheus/prometheus.yml \
    --storage.tsdb.path /var/lib/prometheus/ \
    --web.console.templates=/etc/prometheus/consoles \
    --web.console.libraries=/etc/prometheus/console_libraries \
    --web.listen-address=0.0.0.0:9090
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now prometheus
sudo systemctl status prometheus
```

访问 http://localhost:9090 验证

## 4. Grafana 部署

### 4.1 安装
```bash
# 添加 APT 源（Ubuntu/麒麟）
sudo apt-get install -y apt-transport-https software-properties-common
sudo add-apt-repository "deb https://packages.grafana.com/oss/deb stable main"
wget -qO- https://packages.grafana.com/gpg.key | sudo gpg --dearmor -o /etc/apt/trusted.gpg.d/grafana.gpg

sudo apt-get update
sudo apt-get install -y grafana

sudo systemctl enable --now grafana-server
```

### 4.2 配置数据源
1. 访问 http://localhost:3000（默认 admin/admin）
2. Configuration → Data Sources → Add data source → Prometheus
3. URL: `http://localhost:9090`
4. Save & Test

### 4.3 导入看板
1. Dashboards → Import
2. 上传 `backend/src/main/resources/monitor/grafana-dashboard.json`
3. 选择 Prometheus 数据源
4. Import

## 5. 验证

### 5.1 后端 Actuator
```bash
curl http://localhost:8090/actuator/health
# {"status":"UP"}

curl http://localhost:8090/actuator/prometheus | grep smartgrain_
# smartgrain_stock_tons_total 14500.5
# smartgrain_alerts_total{status="PENDING",...} 4.0
# smartgrain_sensors_total{status="online",...} 8.0
```

### 5.2 Prometheus Targets
访问 http://localhost:9090/targets
应看到 `smart-grain-backend` 状态为 UP

### 5.3 Grafana
访问 http://localhost:3000，导入看板后即可看到 9 个面板

## 6. 告警接入（可选）

如需接入 Alertmanager：
1. 安装：`sudo apt-get install alertmanager`
2. 配置：`/etc/alertmanager/alertmanager.yml`
3. 取消注释 prometheus.yml 中的 alerting 配置
4. 重载 Prometheus

## 7. 生产环境注意事项

- `/actuator/prometheus` 建议加 IP 白名单或 Basic Auth
- Prometheus 数据保留期建议 15-30 天（`--storage.tsdb.retention.time=30d`）
- Grafana 默认密码必须修改
- 关键告警应接入 Alertmanager → 飞书/钉钉/邮件
- 定期备份 `/var/lib/prometheus/` 数据
