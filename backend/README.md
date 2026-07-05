# CampusScore Backend

Spring Boot 2.7 (Java 8) + MyBatis + MySQL 后端服务。

## 启动

```bash
cp src/main/resources/application-example.yml src/main/resources/application-local.yml
# 编辑 application-local.yml
mvn spring-boot:run -Dspring-boot.run.profiles=dev,local
```

## 常用命令

- `mvn clean package -DskipTests` — 打包
- `mvn verify` — 编译 + Spotless + SpotBugs + 单元/切片测试 + JaCoCo 覆盖率闸门
- `mvn test` — 只跑单元测试
- `mvn spotless:apply` — 自动格式化

## 覆盖率门禁（宪章原则二）

- `com.campusscore.service.*` ≥ 85%
- `com.campusscore.web.controller.*` ≥ 80%
- `com.campusscore.persistence.*` ≥ 70%

## 数据库准备

推荐使用 Docker 起 MySQL 8：

```bash
docker run --name csm-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=smxy_class \
  -p 3307:3306 -d mysql:8.0.33 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

- 库名：`smxy_class`；
- 主机端口：`3307`（本地一般已经占用 3306）；
- root 密码：`root`。

启动后端时把这些参数放到 `application-local.yml` 或 `-Dspring.datasource.*`。

## Flyway 迁移

- 生产迁移脚本：`src/main/resources/db/migration/V1..V4__*.sql`；
- **dev profile 额外**：`src/main/resources/db/migration_dev/V5__seed_dev.sql`（示例数据）。

启动后 Flyway 会自动执行 `flyway_schema_history` 里未应用的版本。

## dev profile 种子（V5）

| 类型 | loginName | 密码 | 说明 |
|------|-----------|------|------|
| 教师 | `ttt` | `123456` | 田老师，`tel=18065853353` |
| 教师 | `www` | `123456` | 伍老师 |
| 学生 | `hhh` | `123456` | 黄同学，grade=3 |
| 学生 | `zs` | `123456` | 张三，grade=2 |
| 学生 | `ls` | `123456` | 李四，grade=3 |
| 学生 | `ww` | `123456` | 王五，grade=1 |

课程 / 成绩数据也一并 seed，方便用户故事验证。生产环境**不会**加载 V5。

## 常用 mvn 命令速查

- `mvn -B test` — 全量单元 + 切片测试；
- `mvn -B test -Dtest=SecurityMatrixTest` — 只跑越权矩阵；
- `mvn -B spring-boot:run -Dspring-boot.run.profiles=dev` — dev profile 启动；
- `mvn -B spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-Dspring.datasource.url=jdbc:mysql://localhost:3307/smxy_class?useSSL=false&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true -Dspring.datasource.username=root -Dspring.datasource.password=root"` — 快速用 Docker MySQL 起服务；
- `mvn -B verify` — 覆盖率闸门，PR 前必跑。

## 认证约定（宪章原则一）

- JWT HS256，密钥来自 `campusscore.jwt.secret`（长度 ≥ 32 字节）；
- accessToken 有效期 30 min，refreshToken 7 天；
- 所有 `/api/v1/**` 端点：白名单（`/auth/login`、`/auth/refresh`）之外一律要 JWT；
- 角色矩阵：`/student/**` = STUDENT、`/teacher/**` = TEACHER、`/account/**` = STUDENT ∪ TEACHER。
