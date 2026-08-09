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
- Springdoc OpenAPI

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
| `engine.financial` | 财务测算核心引擎（纯 POJO，红线 R1） |
| `engine.sensitivity` | 敏感性分析引擎（网格重算/敏感系数/临界值） |
| `engine.reverse` | 目标反算引擎（二分迭代 [0.01,10]） |
| `engine.breakeven` | 盈亏平衡引擎（三口径 + 曲线） |
| `engine.montecarlo` | 蒙特卡洛引擎（三角/正态采样，种子可复现） |
| `engine.portfolio` | 组合优化引擎（oj! MIP 0-1 规划） |
| `engine.ai` | AI 打分引擎（六因子加权，可解释） |
| `sensitivity` / `reverse` / `breakeven` / `montecarlo` | 对应 Service/Controller/持久化编排 |
| `comparison` | 多方案横向对比聚合（只读） |
| `portfolio` | 组合优化任务编排（候选池聚合 + 落库） |
| `dashboard` | BI 看板聚合（只读） |
| `risk` | 风险阈值规则与预警事件（FR-02-04） |
| `bpm` | 审批流定义与流程追踪时间线（FR-04-03） |
| `collab` | 评论/变更时间线/在线状态/SSE 推送（FR-04-02） |
| `collaboration` | 场景编辑锁（M1 沿用） |
| `library` | 项目库检索/标签/复盘（FR-03-03） |
| `ai` | AI 运营数据/参数推荐/打分编排（FR-05） |
| `worker` | 异步测算任务轮询执行 |
| `importx` | Excel 导入 |
| `report` | Excel（7 sheet）/PDF 报告生成与下载 |
| `approval` | 审批链执行（实例绑定 flow_def_id） |
| `audit` | 审计事件 + SHA-256 哈希链（R-08） |
| `common` | API 响应和异常处理 |

前端主要文件：

| 文件 | 职责 |
| --- | --- |
| `src/router/index.ts` | 12 条懒加载路由（含 /dashboard、/library） |
| `src/stores/auth.ts` | 登录态和 token 存储 |
| `src/stores/workbench.ts` | 跨页共享工作台状态（Pinia） |
| `src/shared/api/http.ts` | Axios 实例、Bearer token、统一错误处理 |
| `src/shared/types/domain.ts` | 领域类型（含 R-04~R-17 新增视图模型） |
| `src/shared/i18n/display.ts` | 枚举中文映射 |
| `src/views/*.vue` | 12 个业务页面（看板/项目/方案/输入/测算/风险/比选/项目库/报告/治理/审计） |
| `src/components/` | 业务面板（Sensitivity/Reverse/BreakEven/MonteCarlo/RiskAlert/Portfolio/Bpm/Collab/Ai/Comparison） |
| `src/components/charts/` | 图表组件收拢点 |
| `src/layouts/WorkbenchLayout.vue` | 工作台布局（路由驱动菜单） |

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

迁移文件（只增不改，共 14 个）：

- `V1__init_schema.sql` / `V2__auth_seed.sql` / `V3__prd_roles_seed.sql`
- `V4__financial_engine_upgrade.sql`（R-01 投资树/成本分项/参数集扩展）
- `V5__sensitivity_analysis.sql`（R-04）
- `V6__audit_hash_chain.sql`（R-08 审计链）
- `V7__reverse_run.sql`（R-09）
- `V8__monte_carlo_run.sql`（R-11，种子入库）
- `V9__risk_rule_alert.sql`（R-12 + 3 条种子规则）
- `V10__portfolio_run.sql`（R-13）
- `V11__approval_flow_def.sql`（R-14 + 默认三段链模板）
- `V12__collaboration.sql`（R-15 评论/变更/在线）
- `V13__project_library.sql`（R-16 标签/复盘）
- `V14__ai_engine.sql`（R-17 运营记录/模型版本 + SCORING_V1 种子）

主要数据表：

- `sys_user` / `sys_role` / `sys_user_role`
- `project` / `project_tag` / `project_review`
- `scenario` / `parameter_set`
- `investment_item` / `cost_item` / `financing_plan`
- `calculation_task` / `cash_flow_row` / `calculation_result`
- `sensitivity_run` / `sensitivity_cell` / `reverse_run` / `monte_carlo_run`
- `risk_rule` / `risk_alert_event`
- `portfolio_run` / `portfolio_member`
- `approval_flow_def` / `approval_node_def` / `approval_instance`（含 flow_def_id） / `approval_record`
- `scenario_comment` / `scenario_change` / `scenario_presence`
- `ai_operation_record` / `ai_model_version`
- `report_document` / `edit_lock` / `audit_event`（含 prev_hash/hash） / `import_job`

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

### 5.6 OpenAPI

OpenAPI is provided by `springdoc-openapi-starter-webmvc-ui`.

Runtime endpoints:

- `/v3/api-docs`
- `/swagger-ui/index.html`

The contract metadata and JWT Bearer security scheme are configured in `com.sis.iids.docs.OpenApiConfig`. Keep `OpenApiContractTest` updated when adding or renaming core M1 API paths.

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

- 未实现企业级多租户、权限配置后台、复杂审批流设计器（BPM 流程定义已入库可配置，但节点条件规则 `condition_expr` 为预留字段，实例推进仍走固定链）。
- 未实现自动 Workflow Runtime、MCP 集成、多 Agent 自治。
- 前端未提供项目、方案、投资项、融资方案删除入口。
- 前端未提供完整 Excel 模板下载和导入结果明细展示。
- 前端未对审批状态和编辑锁做全局编辑禁用。
- 暂无生产级日志、监控、备份和恢复方案。
- 协同编辑为方案级编辑锁 + 评论/变更/在线推送，未做字段级锁定与冲突合并（R-15 XL 完整范围收敛）。
- AI 打分为规则化加权模型（可解释优先），未接入机器学习训练管线。
- PDF 报告为英文标签摘要（CJK 字体规避），完整中文报告用 Excel 版。
- 性能对标测试以宽松阈值锁定量级（测算 100ms / 蒙特卡洛 1 万次 2.3s / 看板 17ms 实测）；PostgreSQL 集成测试、Docker 实跑、Playwright E2E 因本机无 Docker 未执行，docker-compose.yml 已就绪待补跑。
- `npm audit` 已清零（brace-expansion 修复 + echarts 升级 6.1.0）。

## 9. 下一阶段建议

优先级建议：

1. 在有 Docker 的环境补跑 PostgreSQL 集成测试与 Playwright E2E（登录→建项目→建方案→测算→报告下载）。
2. 保持 OpenAPI 契约测试随核心接口同步更新。
3. 完善 Excel 模板下载、导入预览、错误行展示。
4. 增强审批状态和编辑锁对编辑操作的约束（字段级锁）。
5. BPM 节点条件规则（condition_expr）求值落地（如“参数调整 >±5% 升级投委会”）。
6. 引入日志追踪、操作审计筛选和导出。
7. AI 引擎接入真实训练数据后的模型迭代（当前 SCORING_V1 为规则化基线）。
