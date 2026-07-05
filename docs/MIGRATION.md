# 数据迁移说明（Flyway V1–V5）

> 本文档描述从老 `smxy_class` 库（Struts2 + JSP 时代）迁移到重构后 Spring Boot + MyBatis 架构的数据层演进步骤。

## 迁移脚本

| 版本 | 文件 | 作用 |
|------|------|------|
| V1 | `db/migration/V1__baseline_from_smxy_class.sql` | 从老 `smxy_class.sql` 提取 DDL；**移除** `subjectall` 表 / `trigger` / `intoALL` 存储过程 |
| V2 | `db/migration/V2__rename_and_normalize.sql` | 字段重命名到 `snake_case`；`priviledge` int → `role` VARCHAR('STUDENT'/'TEACHER')；补 `created_at`/`updated_at`；补 CHECK 约束 |
| V3 | `db/migration/V3__migrate_passwords_to_bcrypt.sql` | 新增 `password_hash VARCHAR(72)`；一次性 rehash 老明文密码；删除老 `password` 列 |
| V4 | `db/migration/V4__indexes.sql` | 唯一索引（`LOWER(login_name)`、`(teacher_id,name)`、`(student_id,subject_id)`）；将 `score.subject_id` 外键收紧为 `ON DELETE RESTRICT` |
| V5 | `db/migration_dev/V5__seed_dev.sql` | **仅 dev profile** 加载的示例数据（教师 / 学生 / 课程 / 成绩） |

生产环境的 `spring.flyway.locations` 只包含 `classpath:db/migration`；dev profile 会额外加载 `classpath:db/migration_dev`。

## 关于密码迁移（V3）

老库所有账号的明文密码都是 `123456`。BCrypt 每行需要独立 salt，无法在纯 SQL 里生成——所以我们在 V3 里把**每一行 `password_hash` 都写成 `123456` 的一次性预生成 BCrypt 密文**：

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

这条哈希是 `strength=10` 下 `123456` 的确定性结果，提交到 Git 也**不构成任何机密泄露**（它就等价于告诉大家"迁移后的初始密码是 123456"）。

**教师 / 学生首次登录后必须立即修改密码**——由 US5 "个人信息 / 改密"闭环强制引导（登录页会检测账号仍在初始 hash 时提示改密）。

新账号（US3 学生 CRUD、教师注册）走 `BCryptPasswordEncoder(strength=10)` 生成独立随机 salt，不再共用固定哈希。

## MySQL 版本要求

- **推荐 MySQL 8.0.13+**：V4 使用了 `((LOWER(login_name)))` 函数索引
- **兼容 MySQL 5.7**：需要把 V4 里的函数索引改成 "生成列 + 普通唯一索引"：
  ```sql
  ALTER TABLE `user`
    ADD COLUMN `login_name_lower` VARCHAR(50) GENERATED ALWAYS AS (LOWER(`login_name`)) STORED,
    ADD UNIQUE KEY `uk_user_login_name_lower` (`login_name_lower`);
  ```

## 回滚

Flyway 免费版不支持自动 downgrade。生产回滚步骤：
1. 停应用；
2. 从最近一次备份 `mysqldump` 恢复；
3. `DELETE FROM flyway_schema_history WHERE version > '<target>';`
4. 用旧版本代码重启。

## 首次本地开发环境准备

```bash
# 1. 起 MySQL 8（Docker 示例）
docker run --name campusscore-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=smxy_class \
  -p 3306:3306 -d mysql:8.0.33 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

# 2. 启动后端（dev profile 会自动跑 V1..V5）
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway 会创建 `flyway_schema_history` 表并顺序执行 V1..V5。V5 只在 dev profile 出现，切换到 prod profile 后不会再被应用。

## V3 后的密码运维步骤（T176）

V3 迁移会把老库 `password` 明文列删除，所有账号的 `password_hash` 都被写成 **BCrypt("123456")** 的固定哈希，等价于把**所有旧密码强制重置为 `123456`**。旧密码全部失效。

因此上线 V3 后须按以下步骤运维：

### 1. 上线前通知

- 广告牌 / 邮件 / 系统公告告知全体师生："V3 上线后，登录密码将统一重置为 `123456`，请登录后立即在 `个人中心 → 修改密码` 页面修改。"
- 明确修改密码入口：`/student/password`（学生）、`/teacher/password`（教师）。

### 2. 上线当天

- 执行 V3 迁移；确认 `flyway_schema_history` 中 `V3` 记录为 `success=true`。
- 抽样 3–5 位账号用 `123456` 登录后端 `/api/v1/auth/login`，确认返回 `code=0`。
- 主动登出这些账号（`/api/v1/auth/logout`），把 refresh token 清空，避免误留会话。

### 3. 教师批量重置学生密码（可选强制路径）

若担心学生不改密码：

1. 教师登录 `/teacher/students`；
2. 逐个编辑学生 → 表单里勾选"重置密码"并填入新密码 → 保存；
3. 或使用 `POST /api/v1/teacher/students/{id}/update`，body 里带 `password` 字段。

服务端会把该字段用 `BCryptPasswordEncoder(10)` 重新 hash 保存，与共享哈希无差别。

### 4. 教师端"修改学生密码"闭环

- **前端 UI**：`frontend/src/views/teacher/StudentsView.vue` → 编辑对话框 → `密码（留空则不重置）` 输入框；
- **后端接口**：`POST /api/v1/teacher/students/{id}/update`，`password` 可选；空串或 `null` 表示保留原 hash，非空则重置。

### 5. 修改密码后的会话失效策略

`POST /api/v1/account/password/change` 成功后，前端会：

1. 弹出 `ElMessage.success('密码修改成功，请重新登录')`；
2. 调用 `authStore.logout()` 清 sessionStorage / localStorage；
3. 跳转 `/login?msg=password-changed`，登录页监听此 query 弹一次友好提示。

服务端不做主动 token 撤销（无状态 JWT），依赖前端主动登出 + access token 30 min 自然过期。若需强制撤销，未来可引入 refresh token 黑名单表。

### 6. 应急回滚

若 V3 应用失败：

1. 参考"回滚"小节从备份恢复；
2. **重要**：V3 已删除 `password` 明文列，仅靠代码回滚**无法**恢复原明文密码；必须依赖库级备份。

