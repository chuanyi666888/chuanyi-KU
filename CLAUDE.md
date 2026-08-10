# CLAUDE.md - 项目协作约定

## 项目身份

智慧粮库国产化综合运维与业务管理平台（Smart Grain Warehouse Operations Platform）。
面向银河麒麟 V11 的企业级粮库运维与业务管理平台原型。

## 关键约束（务必遵守）

1. **国产化优先**：所有技术选型必须可在银河麒麟 V11（x86_64 / aarch64）运行
2. **双数据库适配**：业务代码禁止依赖 MySQL/KingbaseES 厂商特有 SQL，方言差异在 Mapper XML 通过 `databaseId` 隔离
3. **RBAC 强制校验**：后端 `@PreAuthorize` 必填，前端 `v-permission` 仅做交互
4. **配置外部化**：密码、密钥、地址等禁止硬编码，统一走环境变量
5. **审计可追溯**：所有写操作必须经 AOP 记录审计日志
6. **事务一致性**：库存等核心数据更新必须事务 + 行锁/乐观锁
7. **真实可运行**：禁止伪造成功响应，IoT 模拟器必须明确标注 `Simulator`

## 开发顺序

严格按 Phase 0 → 8 推进，每个 Phase 完成后：
1. 编译通过
2. 类型检查通过
3. 输出「完成内容 / 修改文件 / 测试结果 / 当前问题 / 下一步」
4. 等待确认后再进入下一阶段

## 代码风格

- 后端：Controller 只做 HTTP 编排，Service 承载业务，Mapper 只做数据访问
- DTO / VO / Entity 严格分离，禁止 Entity 直接出参
- 前端：组件化，API 集中封装，Pinia 管理状态
- 中文注释，英文变量名

## 默认技术决策（已锁定）

| 项 | 选型 |
|---|---|
| 后端 ORM | MyBatis-Plus |
| Phase 1 默认数据库 | MySQL 8.x |
| IoT 数据存储 | MySQL/KingbaseES 按月分区 |
| 前端 UI 库 | Element Plus |
| 大屏可视化 | ECharts + Three.js |

## 默认账号

- admin / Admin@123456（系统管理员）
- keeper01 / Admin@123456（库管员）
- auditor01 / Admin@123456（审计员）

## 命令环境提示

开发机为 Windows + PowerShell，注意：
- 不使用 `&&` / `cd /d`，用 `Set-Location` + `;`
- 不依赖 Windows 特有路径
- 目标部署环境为 Linux（银河麒麟 V11）
