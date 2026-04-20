# Museum Project 开发 README

## 1. 仓库与目标

- GitHub 仓库：`https://github.com/King52HerTz/Museum_Project`
- 本项目当前用于：后端（Java）+ 管理后台（Vue3）并行开发与联调。
- 前端接口文档（重点）：`../frontend_integration_guide.md`

---

## 2. 开发环境准备

### 2.1 必备软件

1. JDK 21（必须）
2. Maven 3.9+（或直接使用项目自带 `mvnw`）
3. Node.js 20+（`web/package.json` 要求）
4. MySQL 8.x（必须）
5. Redis 7.x（必须）
6. FFmpeg（语音/音频相关能力依赖）
7. Git + GitHub Desktop（团队协作）

### 2.2 端口约定

- 后端：`8091`
- 前端（Vite dev）：`8084`
- MySQL：`3306`（本地）/ `13306`（Docker）
- Redis：`6379`

### 2.3 参考文档

- 原项目 Windows 部署文档（可参考安装依赖与环境变量配置）：  
  `https://github.com/joey-zhou/xiaozhi-esp32-server-java/blob/main/docs/WINDOWS_DEVELOPMENT.md`

注意：

1. 本仓库 README 的启动与配置说明优先级更高。
2. 若参考文档与当前仓库不一致（如端口、进程拆分方式），请以当前仓库实际配置为准。

---

## 3. 数据库与缓存配置

### 3.1 当前默认连接（后端）

默认读取 `src/main/resources/application-dev.yml`：

- MySQL URL：`jdbc:mysql://localhost:3306/xiaozhi...`
- MySQL 用户：`xiaozhi`
- MySQL 密码：`123456`
- Redis：`localhost:6379`

说明：

1. 只要你本机 MySQL/Redis 与上述一致，后端可直接启动。
2. 前端同学通常不需要改数据库配置，只需保证后端同学已启动服务。

### 3.2 首次初始化（必须做一次）

#### 第一步：创建库与账号（MySQL）

```sql
CREATE DATABASE IF NOT EXISTS xiaozhi DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'xiaozhi'@'localhost' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON xiaozhi.* TO 'xiaozhi'@'localhost';
FLUSH PRIVILEGES;
```

如果你本机只用 `root`，也可以不创建 `xiaozhi` 用户，但要同步修改后端连接配置。

#### 第二步：启动后端自动建表

本项目使用 `spring.sql.init` 自动执行脚本（不是 Flyway）：

1. `db/init.sql`
2. `db/2026_02_25.sql`
3. `db/2026_04_16_rag.sql`
4. `db/2026_04_18_exhibit_update.sql`

其中 `init.sql` 包含核心表结构 + 演示数据（含 `admin` 用户）。

### 3.3 验证数据库是否连接成功

后端启动后，执行以下检查：

1. 访问 `http://localhost:8091/swagger-ui.html` 能打开。
2. MySQL 中存在关键表：`sys_user`、`sys_device`、`sys_exhibit`、`sys_knowledge_document`。
3. `sys_user` 中存在管理员账号 `admin`。

可在 MySQL 中快速验证：

```sql
USE xiaozhi;
SHOW TABLES;
SELECT username, roleId FROM sys_user WHERE username='admin';
```

### 3.4 默认登录账号

- 用户名：`admin`
- 密码：`123456`

若无法登录，优先排查：

1. 后端是否连接到了正确库（`xiaozhi`）。
2. 初始化 SQL 是否执行成功。
3. 本机 MySQL 账号权限是否足够。

### 3.5 本地个性化配置（可选但推荐）

当你的本机数据库账号、端口、密码与默认值不一致时，再使用本地覆盖配置。

#### 操作步骤

1. 在 `src/main/resources` 创建 `application-local.yml`（仅本地使用）。
2. 写入你自己的连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://YOUR_DB_HOST:YOUR_DB_PORT/YOUR_DB_NAME?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true
    username: YOUR_USERNAME
    password: YOUR_PASSWORD
```

3. 启动时激活本地 profile：

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

注意：

1. 本项目 SQL 脚本大量使用 `xiaozhi` 库名，建议库名保持 `xiaozhi`。
2. `application-local.yml` 仅本地使用，不要提交仓库。

### 3.6 防数据库污染（团队协作必看）

#### 原则

1. 每人使用自己本机数据库实例（或自己 Docker 卷）。
2. 仓库只提交结构/迁移脚本，不提交个人测试数据。

#### 允许提交

1. `db/*.sql` 的建表、改表、索引、幂等迁移脚本。
2. 必要演示 seed（建议独立文件且幂等）。

#### 禁止提交

1. 数据库导出文件（含个人测试数据）。
2. 本地私密配置（密码、密钥、token）。
3. 本地日志、IDE 数据源快照、临时文件。

#### PR 前自检

1. 新 SQL 脚本命名规范：`YYYY_MM_DD_xxx.sql`。
2. 脚本可重复执行（幂等）。
3. 至少做一次空库启动验证，确认仅靠仓库脚本可初始化成功。

---

## 4. 本地启动（源码方式）

### 4.1 启动后端

在项目目录 `xiaozhi-esp32-server-java-main` 执行：

```powershell
.\mvnw.cmd spring-boot:run
```

或：

```bash
./mvnw spring-boot:run
```

启动成功后访问：

- API：`http://localhost:8091`
- Swagger：`http://localhost:8091/swagger-ui.html`

### 4.2 启动前端

在 `web` 目录执行：

```bash
cd web
npm install
npm run dev
```

启动成功后访问：

- 管理后台：`http://localhost:8084`

### 4.3 前端代理说明

`web/vite.config.ts` 已配置：

- `/api` -> `http://localhost:8091`

因此开发环境前端请求建议统一使用相对路径 `/api/...`。

---

## 5. Docker 启动（可选）

在项目根目录执行：

```bash
docker-compose up -d
```

当前 `docker-compose.yml` 端口映射：

- MySQL：`13306 -> 3306`
- 前端：`8085 -> 8084`
- 后端：`8091 -> 8091`

---

## 6. 前端同学重点开发范围

当前项目中，登录与设备管理页面已经有实现；本阶段重点是：

1. 展品管理（`/api/exhibit`）
2. 知识库管理（`/api/knowledge`）
3. 联调面板（`/api/device/context/{deviceId}`、`/api/museum/chat`）

详细接口、参数和调用时序请看：

- `../frontend_integration_guide.md`

---

## 7. GitHub Desktop 协作规范

1. 从 `main` 拉最新代码。
2. 新建功能分支（示例：`feature/exhibit-page`）。
3. 本地开发 + 自测（前后端都可启动）。
4. GitHub Desktop 提交 commit（按功能拆分、信息清晰）。
5. Push 分支到远程。
6. 创建 PR 合并到 `main`。
7. 至少 1 人 review 后再合并，禁止直接推送 `main`。

---

## 8. 常见问题

### 8.1 前端请求 401/403

- 检查是否已登录并携带 `Authorization: Bearer <token>`。

### 8.2 前端请求 500

- 优先看后端控制台日志；
- 再检查 MySQL/Redis 是否启动、连接参数是否正确。

### 8.3 文档上传失败

- 前端必须用 `form-data`，字段名必须是 `file`；
- 不要手动写 `multipart/form-data boundary`。
