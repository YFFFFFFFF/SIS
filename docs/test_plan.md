# 测试计划

本文档定义 M1 版本的验证范围、执行命令和手工验收路径。

## 1. 自动化验证

### 1.1 后端单元和集成测试

命令：

```powershell
cd E:\SIS\backend
mvn -q test
```

覆盖范围：

| 测试文件 | 覆盖内容 |
| --- | --- |
| `FinancialEngineTest` | 财务测算引擎核心指标和现金流 |
| `AuthApiIntegrationTest` | 登录和 token 返回 |
| `RbacIntegrationTest` | RBAC 权限控制 |
| `ProjectApiIntegrationTest` | 项目创建、更新、查询 |
| `ScenarioParameterApiIntegrationTest` | 方案和参数集 |
| `CalculationApiIntegrationTest` | 投资项、融资方案、测算任务和结果 |
| `ExcelImportApiIntegrationTest` | Excel 导入任务 |
| `ReportApiIntegrationTest` | 报告生成和下载 |
| `ApprovalApiIntegrationTest` | 固定审批链 |
| `EditLockApiIntegrationTest` | 编辑锁获取和释放 |
| `AuditEventApiIntegrationTest` | 审计事件查询 |

验收标准：

- Maven 命令退出码为 `0`。
- Surefire 无失败用例。
- H2/Flyway 初始化无阻塞错误。

### 1.2 后端打包

命令：

```powershell
cd E:\SIS\backend
mvn -q -DskipTests package
```

验收标准：

- 命令退出码为 `0`。
- 生成 `target/iids-0.1.0-SNAPSHOT.jar`。

### 1.3 前端类型检查和生产构建

命令：

```powershell
cd E:\SIS\frontend
npm run build
```

验收标准：

- `vue-tsc --noEmit` 通过。
- `vite build` 退出码为 `0`。
- 生成 `frontend/dist`。

当前已知构建警告：

- `@vueuse/core` 中部分 `/* #__PURE__ */` 注释会被 Rollup 移除。
- 主 JS chunk 超过 500 kB，后续可通过路由拆分或 manual chunks 优化。

### 1.4 Compose 配置验证

命令：

```powershell
cd E:\SIS
docker compose config
```

验收标准：

- Compose 文件解析成功。
- `postgres`、`redis`、`backend`、`frontend` 四个服务均出现在配置中。
- backend 数据源环境变量指向 `postgres:5432`。

当前机器验证状态：

- 2026-07-27 执行时 PowerShell 返回 `docker` 命令不可识别。
- 该项未在当前机器完成，需要安装 Docker Desktop 后补跑。

## 2. 手工端到端验收

### 2.1 登录验收

步骤：

1. 启动后端。
2. 启动前端。
3. 打开 `http://localhost:5173`。
4. 使用 `analyst` / `Password123!` 登录。

预期结果：

- 登录成功。
- 进入工作台首页。
- 浏览器请求携带 Bearer token。

### 2.2 项目和方案验收

步骤：

1. 在 `Projects` 创建项目，编码使用唯一值，例如 `P-M1-001`。
2. 选中项目。
3. 在 `Scenarios` 创建方案。
4. 修改方案备注并保存。

预期结果：

- 项目列表刷新并显示新项目。
- 方案列表刷新并显示新方案。
- 修改后再次选中方案能看到最新字段。

### 2.3 输入和测算验收

步骤：

1. 选择方案。
2. 在 `Inputs` 保存参数。
3. 添加一条投资项。
4. 添加一条融资方案。
5. 在 `Calculation` 点击 `Calculate`。
6. 等待任务状态变为 `SUCCESS`。

预期结果：

- 参数保存成功。
- 投资项和融资方案新增成功。
- 测算任务从 `PENDING` 或 `RUNNING` 进入 `SUCCESS`。
- 指标表和现金流表有数据。

### 2.4 报告验收

步骤：

1. 在成功测算任务下点击 `Generate Report`。
2. 点击 `Download Report`。

预期结果：

- 页面提示报告生成成功。
- 浏览器下载 `.xlsx` 文件。
- 文件可以被 Excel 或兼容工具打开。

### 2.5 审批和锁验收

步骤：

1. 选择方案。
2. 点击 `Submit Scenario`。
3. 点击 `Review Approve`。
4. 点击 `Final Approve`。
5. 填写锁持有人并点击 `Acquire Lock`。
6. 点击 `Release Lock`。

预期结果：

- 审批实例创建成功。
- 审批节点按固定链路推进。
- 锁获取后显示过期时间。
- 锁释放后页面清空锁信息。

### 2.6 审计验收

步骤：

1. 切换到 `Audit`。
2. `Target type` 填 `SCENARIO`。
3. `Target id` 填方案 ID。
4. 点击 `Query`。

预期结果：

- 表格返回方案相关操作记录。
- 至少能看到创建、更新、审批或锁相关动作。

## 3. 发布前检查清单

- 后端 `mvn -q test` 通过。
- 后端 `mvn -q -DskipTests package` 通过。
- 前端 `npm run build` 通过。
- Docker 环境可用时 `docker compose config` 通过。
- `.env` 已替换默认密码和 JWT secret。
- 本地报告目录或 Docker volume 可写。
- 使用 `analyst` 账号完成一条端到端测算。
- 使用 `admin` 账号验证 `/api/v1/admin/ping`。

## 4. 当前已执行验证记录

2026-07-27 已执行：

- `E:\SIS\backend`: `mvn -q test`，退出码 `0`。
- `E:\SIS\backend`: `mvn -q -DskipTests package`，退出码 `0`。
- `E:\SIS\frontend`: `npm run build`，退出码 `0`，存在 Vite chunk 和 Rollup 注释警告。

2026-07-27 未完成：

- `E:\SIS`: `docker compose config`，原因是本机 `docker` 命令不可用。

## 5. 后续建议

- 增加前端组件测试或 Playwright E2E。
- 增加 PostgreSQL 真实数据库集成测试。
- 为 Excel 导入补充复杂模板、空值、非法格式测试。
- 为审批链补充重复提交、重复审批、越权审批测试。
- 为报告文件内容增加单元级断言。
