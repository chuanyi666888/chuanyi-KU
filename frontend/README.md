# frontend

智慧粮库平台前端，基于 Vue 3 + TypeScript + Vite。

## 技术栈

- Vue 3.4
- TypeScript 5
- Vite 5
- Vue Router 4
- Pinia 2
- Element Plus 2.6
- ECharts 5 + vue-echarts
- Axios
- Sass

## 启动

```bash
npm install
npm run dev      # 开发，http://localhost:5173
npm run build    # 生产构建到 dist/
npm run preview  # 预览生产构建
npm run type-check
```

开发环境已配置代理：`/api` 与 `/ws` 自动转发到 `http://localhost:8080`。

## 目录结构

```
src/
├── main.ts            # 入口
├── App.vue
├── router/            # 路由 + 守卫
├── stores/            # Pinia（user / app）
├── api/               # Axios 封装 + 各模块 API
├── types/             # 类型定义
├── utils/             # 工具函数（format / permission 指令）
├── layouts/           # 布局壳
├── components/        # 通用组件
├── views/
│   ├── login/
│   ├── dashboard/
│   ├── profile/
│   ├── system/
│   │   ├── user/
│   │   ├── role/
│   │   ├── permission/
│   │   └── audit/
│   └── error/
└── styles/            # 深色科技感全局样式
```

## 主题

采用深色科技感主题（适合监控中心大屏），样式变量见 `src/styles/global.scss`。

## 登录

启动后访问 http://localhost:5173/login，使用以下任一账号登录：

| 用户名 | 密码 | 角色 |
|---|---|---|
| admin | Admin@123456 | 系统管理员（全部权限） |
| keeper01 | Admin@123456 | 库管员 |
| auditor01 | Admin@123456 | 审计员（仅审计日志只读） |
