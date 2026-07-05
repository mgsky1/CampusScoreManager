# CampusScore Frontend

Vue 3 + TypeScript + Vite + Pinia + Element Plus。

## 启动

```bash
cp .env.example .env.development
npm install
npm run dev
```

## 常用命令

- `npm run dev` — 开发服务器
- `npm run build` — 生产构建
- `npm run preview` — 预览生产构建
- `npm run lint` — ESLint 检查
- `npm run typecheck` — vue-tsc 类型检查
- `npm test` — Vitest 单元测试
- `npm run test:coverage` — 覆盖率报告

## 目录

- `src/api/` — HTTP 客户端封装（axios）
- `src/router/` — 路由与守卫
- `src/stores/` — Pinia store
- `src/views/` — 页面级组件（按角色分区）
- `src/components/` — 复用组件
- `src/composables/` — 逻辑复用（`useXxx`）
- `src/types/` — TS 类型（与后端 DTO 对齐）
- `src/styles/` — design tokens 与全局样式

## `.env.example`

```bash
# 后端 API 前缀；Vite dev 会把它作为 axios baseURL
VITE_API_BASE_URL=/api/v1

# 开发时 Vite proxy 目标（可选，若未设则使用 baseURL 直连）
VITE_DEV_PROXY_TARGET=http://localhost:8080
```

拷贝为 `.env.development` 供本地开发使用；生产构建走 `.env.production`。

## npm 命令速查

- `npm install` — 首次装依赖；
- `npm run dev` — Vite dev server（默认端口 5173）；
- `npm run build` — 生产打包，输出到 `dist/`；
- `npm run preview` — 本地预览打包结果；
- `npm run lint` — ESLint + Prettier 检查；
- `npm run typecheck` — `vue-tsc --noEmit` 严格类型检查；
- `npm test` — Vitest 单元测试；
- `npm test -- --coverage` — 生成覆盖率报告到 `coverage/`。

## Element Plus 按需引入注意事项

当前使用**全量引入**（在 `main.ts` 中 `app.use(ElementPlus)`）以简化开发。上线前如需减包体：

1. 安装 `unplugin-vue-components`、`unplugin-auto-import`；
2. 在 `vite.config.ts` 中启用 `ElementPlusResolver`；
3. 从 `main.ts` 移除 `import ElementPlus from 'element-plus'` 与 `app.use(ElementPlus)`；
4. 全站图标 `@element-plus/icons-vue` 也走 auto-import；
5. 打包体积对照 T175 门禁：主 chunk gzip ≤ 300 KB。

**注意**：`ElMessage` / `ElMessageBox` / `ElNotification` 是 API-only 组件，按需引入后需**保留** `import 'element-plus/es/components/message/style/css'` 等样式，否则弹出无样式。

## 类型规范

- 与后端 `contracts/api.md` 对齐的类型放在 `src/types/api.ts`；
- 严禁 `any`；`unknown` + `type guard`；
- HTTP 层封装：`src/utils/http.ts`（axios 实例 + `ApiError`）；调用方直接 `try/catch (e instanceof ApiError)` 分派错误码。
