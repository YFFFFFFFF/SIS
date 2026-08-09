# API 概览

本文档说明 M1 后端 HTTP API 的使用方式。后端统一基础路径为 `/api/v1`。

## OpenAPI 与 Swagger

后端启动后可访问：

- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`

Swagger UI 中需要点击 `Authorize`，填入登录接口返回的 JWT Token。登录接口 `POST /api/v1/auth/login` 是公开接口，其余 `/api/v1/**` 接口默认需要携带：

```http
Authorization: Bearer <token>
```

## 通用规则

- 请求体字段使用 camelCase，例如 `projectType`、`horizonYears`。
- 返回体统一使用响应信封。
- `code` 是机器可判断的状态码。
- `message` 是给用户看的中文消息。
- `data` 是业务数据，错误时通常为 `null`。
- API 枚举值保持英文机器值，例如 `DRAFT`、`SUCCESS`、`INVESTMENT_ANALYST`。
- 前端和文档会展示中文含义，但不会改变 API 枚举契约。

成功响应示例：

```json
{
  "code": "SUCCESS",
  "message": "成功",
  "data": {}
}
```

错误响应示例：

```json
{
  "code": "NOT_FOUND",
  "message": "项目不存在",
  "data": null
}
```

## 认证接口

| 方法 | 路径 | 请求体 | 返回 |
| --- | --- | --- | --- |
| POST | `/auth/login` | `username`, `password` | JWT Token、Token 类型、用户名、显示名、角色列表 |
| GET | `/admin/ping` | 无 | 管理员账号返回 `pong` |

登录请求示例：

```json
{
  "username": "investment_analyst",
  "password": "Password123!"
}
```

本地种子账号：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `analyst` | `Password123!` | `ANALYST`、`INVESTMENT_ANALYST` 兼容账号 |
| `investment_analyst` | `Password123!` | `INVESTMENT_ANALYST` 投资分析师 |
| `finance_specialist` | `Password123!` | `FINANCE_SPECIALIST` 财务专员 |
| `technical_engineer` | `Password123!` | `TECHNICAL_ENGINEER` 技术工程师 |
| `project_manager` | `Password123!` | `PROJECT_MANAGER` 项目管理者 |
| `admin` | `Password123!` | `ADMIN`、`SYSTEM_ADMINISTRATOR` 管理员 |

JWT 返回中的角色为 Spring Security authority 形式，例如 `ROLE_INVESTMENT_ANALYST`。

## M1 角色权限矩阵

| 能力 | 允许角色 |
| --- | --- |
| 创建/更新项目 | `ANALYST`, `INVESTMENT_ANALYST`, `PROJECT_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 创建/更新测算方案 | `ANALYST`, `INVESTMENT_ANALYST`, `PROJECT_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 创建/更新测算参数 | `ANALYST`, `INVESTMENT_ANALYST`, `FINANCE_SPECIALIST`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 创建/修改/删除投资项 | `ANALYST`, `INVESTMENT_ANALYST`, `TECHNICAL_ENGINEER`, `PROJECT_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 创建/修改/删除成本分项 | `ANALYST`, `INVESTMENT_ANALYST`, `FINANCE_SPECIALIST`, `PROJECT_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 创建融资方案 | `ANALYST`, `INVESTMENT_ANALYST`, `FINANCE_SPECIALIST`, `PROJECT_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 创建测算任务 | `ANALYST`, `INVESTMENT_ANALYST`, `PROJECT_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| Excel 导入 | `ANALYST`, `INVESTMENT_ANALYST`, `FINANCE_SPECIALIST`, `TECHNICAL_ENGINEER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 生成报表 | `ANALYST`, `INVESTMENT_ANALYST`, `PROJECT_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 提交审批 | `ANALYST`, `INVESTMENT_ANALYST`, `PROJECT_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 财务复核通过 | `FINANCE_SPECIALIST`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 最终审批通过 | `PROJECT_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 驳回审批 | `FINANCE_SPECIALIST`, `PROJECT_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |
| 获取/释放编辑锁 | 全部 M1 业务角色和管理员角色 |
| 查询审计日志 | `PROJECT_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` |

## 项目接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/projects` | 查询项目列表 |
| GET | `/projects/{id}` | 查询单个项目 |
| POST | `/projects` | 创建项目 |
| PUT | `/projects/{id}` | 更新项目 |

创建项目请求：

```json
{
  "code": "P-001",
  "name": "新能源工厂项目",
  "projectType": "INDUSTRIAL",
  "department": "投资发展部",
  "ownerId": 1,
  "tags": "m1,基准测算",
  "description": "用于 M1 验证的投资项目"
}
```

更新项目请求：

```json
{
  "name": "新能源工厂项目",
  "projectType": "INDUSTRIAL",
  "status": "ACTIVE",
  "department": "投资发展部",
  "ownerId": 1,
  "tags": "m1,基准测算",
  "description": "用于 M1 验证的投资项目"
}
```

项目状态枚举：

| 枚举值 | 中文含义 |
| --- | --- |
| `DRAFT` | 草稿 |
| `ACTIVE` | 启用 |
| `ARCHIVED` | 已归档 |

## 测算方案与参数接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/projects/{projectId}/scenarios` | 查询项目下的测算方案 |
| POST | `/projects/{projectId}/scenarios` | 创建测算方案 |
| GET | `/scenarios/{id}` | 查询单个测算方案 |
| PUT | `/scenarios/{id}` | 更新测算方案 |
| GET | `/scenarios/{id}/parameters` | 查询测算参数集 |
| PUT | `/scenarios/{id}/parameters` | 创建或更新测算参数集 |

创建测算方案请求：

```json
{
  "name": "基准方案",
  "horizonYears": 5,
  "constructionYears": 1,
  "remarks": "M1 基准测算"
}
```

更新测算方案请求：

```json
{
  "name": "基准方案",
  "status": "DRAFT",
  "horizonYears": 5,
  "constructionYears": 1,
  "remarks": "M1 基准测算"
}
```

测算方案状态枚举：

| 枚举值 | 中文含义 |
| --- | --- |
| `DRAFT` | 草稿 |
| `SUBMITTED` | 已提交 |
| `APPROVED` | 已通过 |
| `REJECTED` | 已驳回 |

测算参数请求：

```json
{
  "wacc": 0.1,
  "waccSource": "手工录入基准值",
  "taxRate": 0.25,
  "depreciationYears": 5,
  "residualRate": 0.0,
  "loanRatioLimit": 0.7,
  "pricePerUnit": 140,
  "unitCost": 40,
  "annualOutput": 1000,
  "fixedOperatingCost": 10000,
  "formulaVersion": "fin-m1-1.0.0"
}
```

## 测算输入与测算任务接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/scenarios/{scenarioId}/investment-items` | 查询投资分项（树） |
| POST | `/scenarios/{scenarioId}/investment-items` | 新增投资项 |
| PUT | `/scenarios/{scenarioId}/investment-items/{itemId}` | 修改投资项 |
| DELETE | `/scenarios/{scenarioId}/investment-items/{itemId}` | 删除投资项（级联删除子项） |
| GET | `/scenarios/{scenarioId}/investment-summary` | 投资估算汇总 + 合计校验 |
| GET | `/scenarios/{scenarioId}/cost-items` | 查询成本分项 |
| POST | `/scenarios/{scenarioId}/cost-items` | 新增成本分项 |
| PUT | `/scenarios/{scenarioId}/cost-items/{itemId}` | 修改成本分项 |
| DELETE | `/scenarios/{scenarioId}/cost-items/{itemId}` | 删除成本分项 |
| POST | `/scenarios/{scenarioId}/financing-plans` | 新增融资方案 |
| POST | `/scenarios/{scenarioId}/calculation-tasks` | 创建异步测算任务 |
| GET | `/calculation-tasks/{taskId}` | 查询测算任务状态 |
| GET | `/calculation-tasks/{taskId}/results` | 查询测算指标和现金流结果 |
| GET | `/calculation-tasks/{taskId}/statements?type=` | 按报表类型查询现金流（默认三类全返） |
| GET | `/calculation-tasks/{taskId}/profit-flow` | 利润流向分解（达产年） |
| GET | `/calculation-tasks/{taskId}/loan-schedule` | 还本付息计划 |

投资项请求：

```json
{
  "category": "CONSTRUCTION",
  "name": "建设投资",
  "amount": 200000,
  "yearNo": 0
}
```

常用投资类别：

| 枚举值 | 中文含义 |
| --- | --- |
| `CONSTRUCTION` | 建设投资（一级汇总行） |
| `CONSTRUCTION_BUILDING` | 建筑工程费 |
| `CONSTRUCTION_EQUIPMENT` | 设备购置及安装费 |
| `CONSTRUCTION_OTHER` | 工程建设其他费用 |
| `WORKING_CAPITAL` | 流动资金 |
| `INTEREST_DURING_CONSTRUCTION` | 建设期利息（手录项，引擎自动资本化时忽略） |

投资项可选扩展字段：`itemCode`（分项编码）、`parentId`（父项，构成三级树）、`sortOrder`（排序）。

投资估算汇总返回：

```json
{
  "scenarioId": 1,
  "constructionTotal": 205000.0,
  "interestDuringConstruction": 0.0,
  "workingCapital": 20000.0,
  "totalInvestment": 225000.0,
  "declaredTotalInvestment": null,
  "balanced": true,
  "items": []
}
```

成本分项类别：

| 枚举值 | 中文含义 |
| --- | --- |
| `RAW_MATERIAL` | 外购原材料及燃料动力 |
| `LABOR_MANUFACTURING` | 人工及制造费用 |
| `OTHER_OPERATING` | 其他经营成本 |

成本分项请求：

```json
{
  "category": "RAW_MATERIAL",
  "name": "外购原材料及燃料动力",
  "yearNo": 0,
  "amount": 50000
}
```

融资方案可选扩展字段：`repaymentMethod`（`EQUAL_PRINCIPAL`/`EQUAL_PAYMENT`/`BULLET`）、`graceYears`（宽限期年数）。

参数集新增可选字段：`depreciationPolicy`（折旧政策）、`amortizationYears` / `amortizableAmount`（摊销）、`repaymentMethod`、`taxSchedule`（税率梯度 JSON）、`rampUp`（投产负荷 JSON）。

报表类型（`statements?type=`）：

| 枚举值 | 中文含义 |
| --- | --- |
| `PROJECT_CASH_FLOW` | 项目投资现金流量表 |
| `EQUITY_CASH_FLOW` | 项目资本金现金流量表 |
| `FINANCIAL_PLAN` | 财务计划现金流量表 |

融资方案请求：

```json
{
  "sourceType": "EQUITY",
  "ratio": 1,
  "amount": 220000,
  "interestRate": 0,
  "termYears": 0
}
```

常用资金来源：

| 枚举值 | 中文含义 |
| --- | --- |
| `EQUITY` | 资本金 |
| `LOAN` | 银行贷款 |

测算任务请求：

```json
{
  "taskType": "FINANCIAL",
  "requestKey": "manual-001"
}
```

测算任务状态枚举：

| 枚举值 | 中文含义 |
| --- | --- |
| `PENDING` | 待处理 |
| `RUNNING` | 计算中 |
| `SUCCESS` | 成功 |
| `FAILED` | 失败 |

测算结果包含：

- `task`：任务状态、进度、错误消息等。
- `metrics`：指标编码到数值的映射。
- `cashFlowRows`：分期现金流行，包括流入、流出、净现金流、折现现金流和累计现金流。

核心指标：

| 指标编码 | 中文含义 |
| --- | --- |
| `TOTAL_INVESTMENT` | 总投资 |
| `CONSTRUCTION_INTEREST` | 建设期利息 |
| `NPV` | 净现值 |
| `ROI` | 总投资收益率 |
| `IRR` | 内部收益率 |
| `CAPITAL_NET_PROFIT_RATE` | 资本金净利润率 |
| `STATIC_PAYBACK_YEARS` | 静态回收期 |
| `DYNAMIC_PAYBACK_YEARS` | 动态回收期 |
| `EQUITY_IRR` | 资本金内部收益率 |
| `EQUITY_NPV` | 资本金净现值 |

## 风险与不确定性分析接口（FR-02）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/scenarios/{scenarioId}/sensitivity` | 运行单因素/多因素敏感性分析 |
| GET | `/scenarios/{scenarioId}/sensitivity` | 查询方案下的敏感性分析运行列表 |
| GET | `/sensitivity-runs/{runId}` | 查询单次运行的矩阵结果 |

敏感性分析请求：

```json
{
  "targetMetric": "NPV",
  "variable1": "PRICE",
  "range1": 0.20,
  "steps1": 9,
  "variable2": "UNIT_COST",
  "range2": 0.20,
  "steps2": 9
}
```

- `variable2` 可空，留空即为单因素分析。
- `range` 为波动区间（±比例，如 0.20 = ±20%）；`steps` 为步数（奇数，含基准点）。
- 变量取值：`PRICE`（产品售价）、`UNIT_COST`（单位成本）、`INVESTMENT`（建设投资）、`CONSTRUCTION_PERIOD`（建设工期）。
- 返回：`baseValue`（基准指标值）、`matrix`（因素组合 → 指标值，供热力图）、各因素 `coefficient`（敏感系数）、`criticalFactor`（指标=0 的临界波动比例，线性插值，无穿越为 null）、`level`（`HIGH`/`MEDIUM`/`LOW` 敏感等级，供龙卷风图与解读）。

### 目标反算接口（FR-02-02）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/scenarios/{scenarioId}/reverse-runs` | 运行目标反算（给定目标指标值，反算某变量的临界值） |
| GET | `/scenarios/{scenarioId}/reverse-runs` | 查询方案下的反算运行列表 |
| GET | `/reverse-runs/{runId}` | 查询单次反算运行详情 |

目标反算请求：

```json
{
  "targetMetric": "NPV",
  "targetValue": 0,
  "variable": "PRICE"
}
```

- `targetMetric`：`NPV` / `IRR` / `STATIC_PAYBACK_YEARS`（回收期语义为“不超过目标年”）。
- `variable`：`PRICE`（售价）、`INVESTMENT`（建设投资）、`ANNUAL_OUTPUT`（年产量）、`UNIT_COST`（单位成本）。
- 求解方式：比例因子二分迭代，搜索区间 [0.01, 10]（基准的 1%~1000%），端点指标不可算时自动向内探测。
- 返回：`factor`（求解因子）、`solvedValue`（反算临界值）、`baseValue`（变量基准值）、`achievedValue`（达成指标值）、`feasible`（区间内是否有解）、`iterations`、`sensitivityNote`（敏感性说明）、`boundaryNote`（适用边界与假设声明）。

### 盈亏平衡分析接口（FR-02-02）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/scenarios/{scenarioId}/break-even` | 盈亏平衡分析（BEP 产量 / 产能利用率 / 盈亏平衡售价 + 盈亏平衡图数据） |

只读计算接口，不落库（结论可复算）。

- 口径：达产年（负荷 100%）税前会计口径——总成本 = 经营成本 + 折旧 + 摊销；成本性态按 RAW_MATERIAL=可变、其余=固定分解。
- 返回：`bepOutput`（BEP 产量）、`bepUtilization`（产能利用率）、`bepPrice`（盈亏平衡售价）、`contributionMargin`（单位边际贡献）、`annualFixedCost`（年固定成本）、`curve`（0~150% 产量 11 点的收入/总成本，供盈亏平衡图）、`solvable` 与 `unsolvableReason`（边际贡献≤0 或产量=0 时不可解）、`assumptionNote`（适用边界与假设）。

### 蒙特卡洛概率分析接口（FR-02-03）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/scenarios/{scenarioId}/monte-carlo-runs` | 运行蒙特卡洛模拟（变量分布抽样重算目标指标） |
| GET | `/scenarios/{scenarioId}/monte-carlo-runs` | 查询方案下的蒙特卡洛运行列表 |
| GET | `/monte-carlo-runs/{runId}` | 查询单次运行详情（含直方图/累计曲线） |

蒙特卡洛请求：

```json
{
  "targetMetric": "NPV",
  "iterations": 10000,
  "seed": 42,
  "variables": [
    {"variable": "PRICE", "type": "TRIANGULAR", "min": -0.2, "mode": 0, "max": 0.2},
    {"variable": "UNIT_COST", "type": "NORMAL", "mean": 0, "stdDev": 0.1}
  ]
}
```

- `variable`：`PRICE` / `UNIT_COST` / `INVESTMENT` / `ANNUAL_OUTPUT`；数值为比例扰动（作用于基准值的比例）。
- `type`：`TRIANGULAR`（三角分布，需 `min ≤ mode ≤ max`）/ `NORMAL`（正态分布，需 `stdDev > 0`，按 3σ 截断）。
- `iterations`：1000 ~ 100000（推荐 ≥ 10000）；`seed` 可空，空则随机生成后入库——**同种子 + 同配置复算结果完全一致（红线 R11 可复现）**。
- 返回：`mean`（期望值）、`stdDev`、`probPositive`（P(指标>0)）、`var95`（=P5，95% 置信下界）、`p5/p50/p95`、`min/max`、`histogram`（20 桶等宽直方图）、`cumulative`（21 点累计概率曲线）。
- 性能：实测 1 万次抽样约 1.4 秒（PRD 预算 ≤ 1 分钟）。

### 智能风险预警接口（FR-02-04）

| 方法 | 路径 | 用途 | 权限 |
| --- | --- | --- | --- |
| GET | `/risk-rules` | 阈值规则列表 | 登录 |
| POST | `/risk-rules` | 新建规则 | 管理员 |
| PUT | `/risk-rules/{id}` | 更新规则 | 管理员 |
| DELETE | `/risk-rules/{id}` | 删除规则 | 管理员 |
| POST | `/scenarios/{scenarioId}/risk-alerts/evaluate` | 按方案最新 SUCCESS 测算指标评估全部启用规则 | 写角色 |
| GET | `/risk-alerts?status=OPEN` | 预警事件列表（可按状态过滤） | 登录 |
| GET | `/scenarios/{scenarioId}/risk-alerts` | 方案下预警事件 | 登录 |
| POST | `/risk-alerts/{id}/ack` | 确认预警（仅 OPEN 可确认） | 写角色 |

- 规则字段：`metricCode`（NPV/IRR/回收期/ROI 等）、`direction`（`BELOW` 低于阈值触发 / `ABOVE` 高于触发）、`thresholdValue`、`level`（`RED`/`YELLOW`）、`strategy`（策略建议）、`enabled`。
- 评估语义：触发且无同规则同方案 OPEN 事件 → 新建 OPEN 事件（幂等，不重复触发）；未触发但存在 OPEN 事件 → 标记 `RECOVERED`（恢复留痕）。
- 仪表盘风险信号灯与预警计数由 OPEN 事件驱动（无事件时回退 IRR 占位规则）。
- 内置种子规则：IRR<8% 红灯、IRR<10% 黄灯、NPV<0 红灯。

## BI 仪表盘接口（FR-04）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/dashboard/summary` | BI 看板聚合：KPI（项目数/加权 IRR/总 NPV/预警计数）、NPV-IRR 气泡、阶段分布、行业分布（按投资额）、风险信号灯（占位规则，R-12 后替换）、在途审批待办 |

只读聚合接口，不落库；一次调用返回全部看板数据（首屏性能约束 ≤ 3 秒）。

## 方案比选接口（FR-03）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/projects/{projectId}/comparison` | 多方案横向对比矩阵与排序建议 |

对比矩阵为只读聚合（不落库）：取项目下各方案最新一次 SUCCESS 测算任务的指标生成。

- `scenarios`：方案列（含未测算方案，`calculated=false`、指标值为 null）
- `metrics`：指标行——总投资额、NPV、IRR、静态/动态回收期、ROI、资本金净利润率 + 风险等级占位行（待 R-14）；`direction` 标记越大越好（`HIGHER`）/越小越好（`LOWER`）/不标记（`NONE`）；`bestScenarioIds` 为该指标最优方案（支持并列）
- `ranking`：排序建议，按 NPV 降序（并列时按 IRR 降序），未测算方案不参与

### 组合优化接口（FR-03-02）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/portfolio-runs` | 资金池/数量约束下的 0-1 整数规划组合优选（落库留痕） |
| GET | `/portfolio-runs/{runId}` | 查询组合优化运行详情（成员 + 帕累托前沿） |

组合优化请求：

```json
{"budget": 500000, "maxCount": 3}
```

- 候选池：全部已测算成功方案的最新一次指标（NPV/IRR/总投资，与 R-05/R-07 同口径）；`maxCount` 可空 = 不限。
- 模型：`max Σ npvᵢ·xᵢ`，s.t. `Σ invᵢ·xᵢ ≤ budget`、`Σ xᵢ ≤ maxCount`、`xᵢ ∈ {0,1}`（oj! Algorithms MIP，D1 选型 A）。
- 返回：`totalNpv` / `totalInvestment` / `explanation`（入选名单、资金利用率、边际 NPV/投资比）/ `members`（入选 rank 升序、未入选 NPV 降序）/ `frontier`（21 档预算 → 最优组合 NPV，帕累托前沿）。


## 导入与报表接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/scenarios/{scenarioId}/import/excel` | 上传 Excel 文件，表单字段名为 `file` |
| GET | `/import-jobs/{jobId}` | 查询导入任务 |
| POST | `/calculation-tasks/{taskId}/reports` | 为成功测算任务生成报告，`format` 查询参数：`EXCEL`（默认）/`PDF` |
| GET | `/reports/{reportId}/download` | 下载已生成报告（按文档类型返回 Excel 或 PDF Content-Type） |

Excel 导入模板当前使用固定工作表名称：

- `Parameters`
- `InvestmentItems`
- `FinancingPlans`

导出的 Excel 报表为结构化多工作表（FR-01-06）：

- `报告说明`（参数集 ID、公式版本、引擎版本、输入哈希、敏感性结论、投资建议——引用参数版本与数据来源）
- `项目概况`
- `指标汇总`
- `投资估算`
- `现金流量表`
- `利润流向`
- `还本付息`

PDF 版为摘要报告（OpenPDF 内置字体，正文英文标签）：封面（版本与来源引用 + 结论）→ 指标摘要 → 投资估算 → 融资前现金流 → 利润流向 → 还本付息。完整中文报告请使用 Excel 版。

下载接口返回二进制内容，并设置 `Content-Disposition: attachment`。

## 审批、编辑锁与审计接口

| 方法 | 路径 | 请求体 | 用途 |
| --- | --- | --- | --- |
| POST | `/scenarios/{scenarioId}/approval/submit` | 可选 `comment` | 提交测算方案进入固定审批链 |
| POST | `/approval-instances/{instanceId}/review/approve` | 可选 `comment` | 财务复核通过 |
| POST | `/approval-instances/{instanceId}/approve` | 可选 `comment` | 项目经理最终审批通过 |
| POST | `/approval-instances/{instanceId}/reject` | 可选 `comment` | 驳回审批流程 |
| POST | `/scenarios/{scenarioId}/lock` | `holderId`, `holderName`, `ttlMinutes` | 获取测算方案编辑锁 |
| DELETE | `/scenarios/{scenarioId}/lock` | `holderId` | 释放测算方案编辑锁 |
| GET | `/audit-events?targetType={type}&targetId={id}` | 无 | 查询审计日志（含 prevHash/hash 链式哈希） |
| GET | `/audit-events/chain/verify` | 无 | R-08 审计链完整性校验：返回 `totalEvents`/`linkedEvents`/`intact`/`brokenCount`/`brokenEventIds`。V6 前历史事件未纳入链（hash 为 NULL）自动跳过；任一事件被篡改（内容或 prev_hash 断链）都会计入 broken |

审批备注请求：

```json
{
  "comment": "审批通过"
}
```

获取编辑锁请求：

```json
{
  "holderId": 1,
  "holderName": "投资分析师",
  "ttlMinutes": 30
}
```

释放编辑锁请求：

```json
{
  "holderId": 1
}
```

审批状态与节点中文展示由前端完成，API 仍返回英文机器值，例如 `IN_REVIEW`、`IN_APPROVAL`、`APPROVED`。

### BPM 可配置审批流接口（FR-04-03，R-14）

| 方法 | 路径 | 用途 | 权限 |
| --- | --- | --- | --- |
| GET | `/admin/approval-flows` | 审批流定义列表（含节点链） | 登录 |
| GET | `/admin/approval-flows/{id}` | 审批流定义详情 | 登录 |
| POST | `/admin/approval-flows` | 新建审批流（code 唯一、节点 seq 从 1 连续） | 管理员 |
| PUT | `/admin/approval-flows/{id}` | 更新审批流（节点链全量替换） | 管理员 |
| DELETE | `/admin/approval-flows/{id}` | 删除审批流（默认模板不可删） | 管理员 |
| GET | `/approval-instances/{instanceId}/timeline` | 流程追踪时间线（节点进度 current/passed + 操作事件流） | 登录 |

- 默认模板 `DEFAULT_REVIEW_CHAIN`（提交 → 财务复核 → 项目经理审批）随 V11 迁移入库；提交审批时实例自动绑定默认流 `flow_def_id`，历史未绑定实例在时间线接口回退默认模板展示。
- 节点字段：`nodeCode`/`nodeName`/`seq`/`approverRole`/`conditionExpr`（条件规则预留，如“参数调整 >±5% 升级投委会”）；设 `isDefault=true` 时自动取消其他流的默认标记。

### 协同编辑接口（FR-04-02，R-15）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/scenarios/{scenarioId}/collab/stream` | SSE 订阅（评论/变更/在线事件即时推送，D2 选型 A） |
| GET | `/scenarios/{scenarioId}/comments` | 评论列表（时间升序） |
| POST | `/scenarios/{scenarioId}/comments` | 发表评论（`content` 支持 @提及自动解析，`parentId` 可选回复） |
| GET | `/scenarios/{scenarioId}/changes` | 变更时间线（版本号 v1, v2... 方案内递增，评论自动留痕） |
| GET | `/scenarios/{scenarioId}/presence` | 在线用户列表（2 分钟心跳窗口） |
| POST | `/scenarios/{scenarioId}/presence` | 在线心跳（`userId`/`userName`，同人去重，返回最新在线列表） |

- SSE 事件名：`connected` / `comment` / `change` / `presence`；前端 EventSource 订阅（token 经查询参数传递）。
- 变更类型：`COMMENT_ADDED` / `FIELD_UPDATED` / `LOCK_ACQUIRED` / `LOCK_RELEASED` / `CALCULATION_RUN` / `APPROVAL_ACTION`。

### 项目库与知识沉淀接口（FR-03-03，R-16）

| 方法 | 路径 | 用途 | 权限 |
| --- | --- | --- | --- |
| GET | `/project-library?status=&projectType=&tag=&keyword=` | 项目库多维检索（四维过滤 + 最新测算 NPV/IRR + hasReview） | 登录 |
| GET | `/projects/{projectId}/tags` | 项目标签列表 | 登录 |
| PUT | `/projects/{projectId}/tags` | 设置项目标签（全量替换，小写归一去重） | 写角色 |
| POST | `/projects/{projectId}/review` | 保存项目复盘（对照方案须属本项目） | 写角色 |
| GET | `/projects/{projectId}/review` | 项目复盘详情（计划 vs 实际 + 偏差率） | 登录 |

- 复盘对照：`scenarioId` 指定本项目的测算方案，计划指标取其最新 SUCCESS 测算；`npvDeviation`/`irrDeviation` = (实际−计划)/|计划|。

### AI 决策引擎接口（FR-05，R-17）

| 方法 | 路径 | 用途 | 权限 |
| --- | --- | --- | --- |
| GET | `/projects/{projectId}/ai/operation-records` | 历史运营数据列表 | 登录 |
| POST | `/projects/{projectId}/ai/operation-records` | 录入历史运营数据（`verified=true` 才纳入训练库） | 写角色 |
| GET | `/scenarios/{scenarioId}/ai/param-recommendation` | 智能参数推荐（WACC/售价/成本/敏感性区间 + 依据来源） | 登录 |
| GET | `/scenarios/{scenarioId}/ai/score` | 智能打分（六因子加权总分 + 标签 + 逐因子明细 + 免责声明） | 登录 |

- 打分模型 `SCORING_V1`（V14 种子）：六因子——NPV 分档、IRR-WACC 溢价线性、回收期占测算期比、敏感性等级、BEP 产能利用率、历史复盘偏差率；权重 0.25/0.25/0.15/0.15/0.10/0.10；标签阈值 ≥70 `RECOMMEND`、50~70 `CAUTION`、<50 `HOLD`。
- 反哺链路：已校验运营记录 + 项目库复盘 → 偏差率中位数 → 参数推荐区间与打分因子。
- 可解释性约束：每因子输出 `explain` 打分依据；响应含 `disclaimer`（不构成投资建议，不替代人工决策）。