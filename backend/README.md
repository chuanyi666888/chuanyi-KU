# backend

智慧粮库平台后端服务。

## 技术栈

- Java 17
- Spring Boot 3.2.5
- Spring Security + JWT (jjwt 0.12)
- MyBatis-Plus 3.5
- MySQL 8.x（Phase 7 适配 KingbaseES V8）
- Redis 7
- WebSocket
- Actuator + Micrometer Prometheus
- Lombok / Hutool / Commons Lang3
- springdoc-openapi（Swagger UI）

## 启动

```bash
# 1. 启动 MySQL 与 Redis
# 2. 初始化数据库
mysql -uroot -p < src/main/resources/db/migration/mysql/V1__init_schema.sql

# 3. 启动服务（开发环境）
mvn spring-boot:run

# 4. 访问
# API: http://localhost:8080/api/v1/...
# Swagger UI: http://localhost:8080/swagger-ui.html
# Actuator: http://localhost:8080/actuator/health
```

## 配置外部化

通过环境变量覆盖（生产环境）：

| 变量 | 说明 | 默认值 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Profile | dev |
| `DB_HOST` | 数据库地址 | 127.0.0.1 |
| `DB_PORT` | 数据库端口 | 3306 |
| `DB_NAME` | 数据库名 | smart_grain |
| `DB_USERNAME` | 数据库用户 | root |
| `DB_PASSWORD` | 数据库密码 | root |
| `REDIS_HOST` | Redis 地址 | 127.0.0.1 |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `REDIS_PASSWORD` | Redis 密码 | （空） |
| `JWT_SECRET` | JWT 密钥（≥32 字节） | dev 默认值（生产必须替换） |

## 目录结构

```
src/main/java/com/smartgrain/
├── SmartGrainApplication.java
├── common/         # 统一响应、异常、常量、工具
├── config/         # Spring / Security / MyBatis / Redis 配置
├── security/       # JWT、UserPrincipal、登录限流
├── domain/         # 领域实体（按子域分包）
│   ├── iam/        # 用户/角色/权限
│   └── audit/      # 审计日志
├── controller/v1/  # REST API
├── service/        # 业务逻辑
├── mapper/         # MyBatis 数据访问
├── aspect/         # AOP（审计日志）
└── ...

src/main/resources/
├── application.yml            # 公共配置
├── application-dev.yml        # 开发
├── application-prod.yml       # 生产
├── mapper/                    # MyBatis XML
└── db/migration/
    ├── mysql/V1__init_schema.sql
    └── kingbase/V1__init_schema.sql  (Phase 7)
```

## API 端点

详见 `src/main/java/com/smartgrain/controller/v1/` 下各 Controller。

启动后访问 Swagger UI 查看完整接口文档。

## 默认账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| admin | Admin@123456 | 系统管理员 |
| keeper01 | Admin@123456 | 库管员 |
| auditor01 | Admin@123456 | 审计员 |
