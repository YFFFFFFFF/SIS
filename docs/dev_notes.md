# 开发说明

本文档面向后续开发者，说明 M1 代码结构、运行配置、关键约定和已知限制。

## 1. 技术栈

后端：

- Java 17
- Spring Boot 3.3.5
- Spring MVC
- Spring Security
- Spring Data JPA
- Flyway
- H2 本地开发数据库
- PostgreSQL Docker/部署数据库
- Apache POI
- JJWT

前端：

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Element Plus
- ECharts
- Axios

部署基线：

- `docker-compose.yml`
- 后端多阶段 Maven Dockerfile
- 前端 Vite build + nginx Dockerfile
- PostgreSQL 16
- Redis 7

## 2. 目录结构

```text
E:\SIS
├── backend
│   ├── src\main\java\com\sis\iids
│   ├── src\main\resources
│   └── src\test\java\com\sis\iids
├── frontend
│   ├── src
│   ├── vite.config.ts
│   └── package.json
├── docs
├── docker-compose.yml
└── README.md
```

后端主要包：

| 包 | 职责 |
| --- | --- |
| `auth` | 用户、角色、登录 |
| `security` | JWT、RBAC、安全过滤器 |
| `project` | 项目管理 |
| `scenario` | 测算方案和参数集 |
| `calculation` | 投资项、融资方案、测算任务、结果 |
| `engine.financial` | 财务测算核心引擎 |
| `worker` | 异步测算任务轮询执行 |
| `importx` | Excel 导入 |
| `report` | Excel 报告生成与下载 |
| `approval` | 固定审批链 |
| `collaboration` | 场景编辑锁 |
| `audit` | 审计事件 |
| `common` | API 响应和异常处理 |

前端主要文件：

| 文件 | 职责 |
| --- | --- |
| `src/router/index.ts` | 登录页和工作台路由 |
| `src/stores/auth.ts` | 登录态和 token 存储 |
| `src/shared/api/http.ts` | Axios 实例、Bearer token、统一错误处理 |
| `src/views/LoginView.vue` | 登录页 |
| `src/views/WorkbenchHome.vue` | M1 业务工作台 |
| `src/components/MetricChart.vue` | 测算指标图表 |
| `src/layouts/WorkbenchLayout.vue` | 工作台布局 |

## 3. 配置说明

### 3.1 后端配置

基础配置文件：

- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml`

关键配置：

| 配置 | 含义 | 默认值 |
| --- | --- | --- |
| `server.port` | 后端端口 | `8080` |
| `spring.profiles.active` | 默认 profile | `dev` |
| `iids.security.jwt-secret` | JWT 签名密钥 | Demo 值 |
| `iids.security.jwt-expire-seconds` | token 有效期 | `86400` |
| `iids.report-dir` | 报告输出目录 | `./data/reports` |
| `iids.upload-dir` | 上传文件目录 | `./data/uploads` |
| `iids.worker.enabled` | 是否启用异步测算 worker | `true` |
| `iids.worker.poll-ms` | worker 轮询间隔 | `1000` |

本地 `dev` profile 使用 H2 内存数据库。Compose 通过环境变量覆盖数据源为 PostgreSQL：

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/iids
SPRING_DATASOURCE_USERNAME=iids
SPRING_DATASOURCE_PASSWORD=...
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

### 3.2 前端配置

Vite 开发代理：

```text
/api -> http://localhost:8080
```

前端 API base URL：

```text
VITE_API_BASE_URL
```

未设置时默认使用：

```text
/api/v1
```

Docker nginx 会把 `/api/` 反向代理到：

```text
http://backend:8080/api/
```

## 4. 数据库

迁移文件：

- `V1__init_schema.sql`
- `V2__auth_seed.sql`

主要数据表：

- `sys_user`
- `sys_role`
- `sys_user_role`
- `project`
- `scenario`
- `parameter_set`
- `investment_item`
- `financing_plan`
- `calculation_task`
- `cash_flow_row`
- `calculation_result`
- `report_document`
- `approval_instance`
- `approval_record`
- `edit_lock`
- `audit_event`
- `import_job`

种子账号：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `analyst` | `Password123!` | `ANALYST` |
| `admin` | `Password123!` | `ADMIN` |

## 5. 后端开发约定

### 5.1 API 返回

普通 JSON API 统一返回：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

下载类接口可以直接返回 `ResponseEntity<byte[]>`。

### 5.2 鉴权

规则：

- `POST /api/v1/auth/login` 免登录。
- `/api/v1/admin/**` 需要 ADMIN。
- 其他 `/api/v1/**` 需要登录。

新增 API 时优先放在 `/api/v1` 下，并确认是否需要额外角色控制。

### 5.3 异常处理

业务异常使用 `BusinessException` 和 `ErrorCode`，由 `GlobalExceptionHandler` 转换为统一响应。

### 5.4 审计

核心业务动作应写入 `audit_event`，至少包含：

- `action`
- `targetType`
- `targetId`
- `beforeValue`
- `afterValue`

### 5.5 财务引擎

财务测算核心在 `engine.financial` 包。新增公式时建议：

1. 先补 `FinancialEngineTest`。
2. 在 `FinancialInput` 增加明确字段。
3. 在 `FinancialEngine` 中实现计算。
4. 在 `CalculationService` 中持久化新增指标。
5. 在前端 `MetricChart` 或指标表中展示。

## 6. 前端开发约定

### 6.1 API 调用

统一使用 `src/shared/api/http.ts` 中的封装：

- `apiGet`
- `apiPost`
- `apiPut`
- `apiDelete`
- `apiDownload`

不要在页面组件中重复创建 Axios 实例。

### 6.2 登录态

token localStorage key：

```text
iids.auth.token
```

Pinia store 在 `src/stores/auth.ts`。

### 6.3 页面结构

M1 当前是单页工作台。后续如果页面复杂度继续增长，建议按业务域拆分为：

- `ProjectView`
- `ScenarioView`
- `InputView`
- `CalculationView`
- `GovernanceView`
- `AuditView`

拆分前应保持 API 类型和请求方法不变，避免一次性重构业务逻辑。

## 7. 构建和运行

后端测试：

```powershell
cd E:\SIS\backend
mvn -q test
```

后端打包：

```powershell
cd E:\SIS\backend
mvn -q -DskipTests package
```

前端依赖：

```powershell
cd E:\SIS\frontend
npm install
```

前端开发：

```powershell
cd E:\SIS\frontend
npm run dev
```

前端构建：

```powershell
cd E:\SIS\frontend
npm run build
```

Compose：

```powershell
cd E:\SIS
docker compose up --build
```

## 8. 已知限制

- M1 未实现企业级多租户、权限配置后台、复杂审批流设计器。
- M1 未实现自动 Workflow Runtime、MCP 集成、多 Agent 自治。
- 前端工作台为 M1 操作入口，尚未拆分复杂页面。
- 前端未提供项目、方案、投资项、融资方案删除入口。
- 前端未提供完整 Excel 模板下载和导入结果明细展示。
- 前端未对审批状态和编辑锁做全局编辑禁用。
- 暂无 OpenAPI/Swagger 文档生成。
- 暂无生产级日志、监控、备份和恢复方案。
- `npm install` 当前报告 5 个安全审计项，未执行 `npm audit fix --force`，因为可能引入破坏性依赖升级。

## 9. 下一阶段建议

优先级建议：

1. 增加 OpenAPI 文档和接口契约测试。
2. 前端拆分工作台页面并补 E2E 测试。
3. 完善 Excel 模板下载、导入预览、错误行展示。
4. 增强审批状态和编辑锁对编辑操作的约束。
5. 增加 PostgreSQL 集成测试环境。
6. 引入日志追踪、操作审计筛选和导出。
7. 优化前端 chunk 体积。
