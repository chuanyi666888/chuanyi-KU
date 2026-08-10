# 智慧粮库国产化综合运维与业务管理平台

> Smart Grain Warehouse Operations Platform

面向智慧粮库场景的国产化综合管理平台原型，目标运行在**银河麒麟 V11**操作系统（x86_64 / aarch64）。

## 项目结构

```
smart-grain-platform/
├── backend/        Spring Boot 3 + Java 17 + MyBatis-Plus + MySQL/KingbaseES + Redis
├── frontend/       Vue 3 + TypeScript + Vite + Element Plus + ECharts
├── deploy/         银河麒麟 V11 部署脚本与 systemd 配置（Phase 7）
├── backup-tool/    数据库备份恢复工具（Phase 8）
├── docs/           架构 / 数据库 / API / 部署文档
└── docker/         Docker 编排（可选）
```

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.2 · Spring Security · JWT · MyBatis-Plus 3.5 · MySQL 8 / KingbaseES V8 · Redis 7 · WebSocket · Actuator |
| 前端 | Vue 3.4 · TypeScript · Vite 5 · Pinia · Vue Router · Element Plus · ECharts · Three.js（Phase 6） |
| 运维 | Prometheus · Node Exporter · Grafana · Nginx · systemd |
| 部署 | 银河麒麟 V11 · x86_64 / aarch64 |

## 默认账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 系统管理员 | admin | Admin@123456 |
| 库管员 | keeper01 | Admin@123456 |
| 审计员 | auditor01 | Admin@123456 |

## 开发

### 后端

```bash
cd backend
mvn spring-boot:run
# 默认端口 8080
# 需要 MySQL(3306) 与 Redis(6379) 已启动
# 数据库初始化脚本：src/main/resources/db/migration/mysql/V1__init_schema.sql
```

### 前端

```bash
cd frontend
npm install
npm run dev
# 默认端口 5173，已配置 /api 与 /ws 代理到后端
```

## 开发阶段

- [x] Phase 0 项目审计与设计
- [x] Phase 1 基础项目（Spring Boot + Vue + DB + Redis + JWT + RBAC）
- [ ] Phase 2 粮库业务（粮库 / 粮仓 / 批次 / 入出库 / 库存）
- [ ] Phase 3 IoT（传感器 / 模拟器 / WebSocket）
- [ ] Phase 4 告警系统
- [ ] Phase 5 监控系统（Prometheus + Grafana + Actuator）
- [ ] Phase 6 可视化大屏（Dashboard + ECharts + Three.js）
- [ ] Phase 7 国产化适配（麒麟 V11 + KingbaseES + systemd）
- [ ] Phase 8 备份恢复（backup-tool + rsync + 恢复演练）

## 安全约定

- 所有密码 BCrypt 加密
- JWT 密钥、数据库密码、Redis 密码通过环境变量外部化，禁止入库 Git
- 后端 `@PreAuthorize` + 自定义 `hasPermission` 强制权限校验
- 前端 `v-permission` 指令仅做交互隐藏
- 操作审计 AOP 自动记录所有写操作
