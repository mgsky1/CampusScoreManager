# 学生成绩信息管理系统 · Campus Score Manager

> 本项目已完成**架构重构**：原 Struts2 + JSP + JDBC 单体已下线，
> 现有代码库为 **Spring Boot 2.7（Java 8）+ Vue 3 + TypeScript** 前后端分离架构。
>
> 老代码已在 T182 中删除；旧库 DDL 归档至 [`docs/legacy/smxy_class.sql`](docs/legacy/smxy_class.sql) 仅供参考。

## 目录结构

```
CampusScoreManager/
├── backend/       # Spring Boot 2.7 (Java 8) + MyBatis + MySQL
├── frontend/      # Vue 3 + TypeScript + Vite + Element Plus
├── docs/          # 需求文档 + 迁移说明
├── specs/         # spec-kit 生成的 spec / plan / tasks / contracts
├── scripts/       # 冒烟测试脚本等
└── .specify/      # spec-kit 元数据
```

## 快速开始

**详细验证脚本**：[`specs/001-score-system-rebuild/quickstart.md`](specs/001-score-system-rebuild/quickstart.md)

### 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 8（本地开发可用 8/11/17/22，编译目标固定 1.8）|
| Maven | 3.6+ |
| Node.js | **≥ 18 LTS** |
| npm | **≥ 9** |
| MySQL | 8.0+ 或 5.7 |

### 数据库

数据库由 Flyway 在后端启动时自动初始化：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS smxy_class DEFAULT CHARSET utf8mb4;"
```

无需手工执行 SQL 脚本；迁移文件位于 `backend/src/main/resources/db/migration/`。

### 启动后端

```bash
cd backend
cp src/main/resources/application-example.yml src/main/resources/application-local.yml
# 编辑 application-local.yml，填 MySQL 用户名 / 密码 / JWT secret
mvn spring-boot:run -Dspring-boot.run.profiles=dev,local
```

### 启动前端

```bash
cd frontend
cp .env.example .env.development
npm install
npm run dev
# 访问 http://localhost:5173
```

## 规范与治理

- 项目宪章：[`.specify/memory/constitution.md`](.specify/memory/constitution.md)（v2.0.0）
- 功能规范：[`specs/001-score-system-rebuild/spec.md`](specs/001-score-system-rebuild/spec.md)
- 接口契约：[`specs/001-score-system-rebuild/contracts/api.md`](specs/001-score-system-rebuild/contracts/api.md)
- 任务清单：[`specs/001-score-system-rebuild/tasks.md`](specs/001-score-system-rebuild/tasks.md)

## 关于原系统

原项目为 JavaWeb 课程设计作品，实现了学生 / 教师两类角色下的成绩管理功能。
原始功能说明参见 [`docs/学生成绩管理系统课程设计功能说明.docx`](docs/学生成绩管理系统课程设计功能说明.docx)。

## 贡献流程

1. 从 `main` 切分支：`git switch -c feature/<short-slug>`；
2. 遵循 [`.specify/memory/constitution.md`](.specify/memory/constitution.md) 的四条原则（代码质量 / 测试标准 / 用户体验一致性 / 性能预算）；
3. 修改前先跑 `cd backend && mvn -B test` 与 `cd frontend && npm test`，确认基线绿色；
4. 使用 `.agents/skills/` 下的 spec-kit 技能：
   - 新特性走 `speckit-specify` → `speckit-plan` → `speckit-tasks` → `speckit-implement`；
   - 复盘走 `speckit-analyze` / `speckit-converge`；
5. 提交前跑：
   - `cd backend && mvn verify`（包含 Spotless、SpotBugs、单元/切片测试、JaCoCo 覆盖率闸门）；
   - `cd frontend && npm run lint && npm run typecheck && npm test`；
6. 通过 GitHub PR 合入，标题格式：`feat(user-story): xxx` / `fix(backend): xxx` / `chore: xxx`。

## CI

CI 会在每个 PR 上跑以下门禁（对齐宪章原则一~四）：

- `backend/`：`mvn -B verify` — Spotless / SpotBugs / 单元 + 切片测试 / 覆盖率闸门；
- `frontend/`：`npm ci && npm run lint && npm run typecheck && npm test`；
- `scripts/smoke.sh` 冒烟：拉起 backend + frontend + MySQL，验证 quickstart §5.1–§5.3；
- 打包体积门禁（T175）：前端主 chunk gzip ≤ 300 KB。

失败任一门禁即阻塞合入。
