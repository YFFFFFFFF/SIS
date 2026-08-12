# 功能完整性矩阵

> 基线日期：2026-08-11
> 状态口径：完成 / 受限完成 / 待验证 / 未完成。只有页面、接口、数据变化、异常处理和测试形成闭环时才标记为完成。

| 编号 | 用户任务 | 前置条件 | 前端入口 | 接口与数据变化 | 自动化覆盖 | 状态 |
|---|---|---|---|---|---|---|
| F01 | 登录系统 | 预置用户 | `/login` | `POST /auth/login` | `AuthApiIntegrationTest` | 完成 |
| F02 | 创建和维护项目 | 对应业务角色 | `/projects` | 项目 CRUD、审计事件 | `ProjectApiIntegrationTest` | 完成 |
| F03 | 创建和维护方案 | 已选择项目 | `/scenarios` | 方案 CRUD、锁和协作数据 | `ScenarioParameterApiIntegrationTest` | 完成 |
| F04 | 维护测算输入 | 已选择方案 | `/inputs` | 参数、投资、成本、融资 CRUD | `CalculationExtendedApiIntegrationTest` | 完成 |
| F05 | 执行财务测算 | 输入完整 | `/calculation` | 任务、指标和现金流持久化 | `CalculationApiIntegrationTest` | 完成 |
| F06 | 查看财务指标 | 成功测算 | `/calculation` | NPV、IRR、回收期、ROI | `FinancialEngineTest` | 待黄金样例复核 |
| F07 | 敏感性分析 | 成功测算 | `/risk` | 运行和网格结果 | `SensitivityApiIntegrationTest` | 完成 |
| F08 | 目标反算 | 输入完整 | `/risk` | 反算运行和结果 | `ReverseApiIntegrationTest` | 完成 |
| F09 | 盈亏平衡分析 | 输入完整 | `/risk` | 实时计算 BEP | `BreakEvenApiIntegrationTest` | 完成 |
| F10 | 蒙特卡洛分析 | 输入完整 | `/risk` | 保存种子和统计结果 | `MonteCarloApiIntegrationTest` | 完成 |
| F11 | 风险预警 | 成功测算 | `/risk` | 评估、确认和恢复 | `RiskApiIntegrationTest` | 完成 |
| F12 | 多方案比较 | 同项目多方案 | `/compare` | 聚合最新成功测算 | `ComparisonApiIntegrationTest` | 完成 |
| F13 | 组合优化 | 存在候选方案 | `/compare` | 组合运行和成员结果 | `PortfolioApiIntegrationTest` | 完成 |
| F14 | Excel 报告 | 成功测算 | `/reports` | 报告元数据和文件 | `ReportApiIntegrationTest` | 完成 |
| F15 | 中文 PDF 报告 | 成功测算 | `/reports` | PDF 文件和下载 | `ReportApiIntegrationTest` | 完成（待版式人工验收） |
| F16 | 固定审批流 | 有方案且角色匹配 | `/governance` | 提交→复核→审批/驳回 | 审批和 BPM 集成测试 | 完成（固定流程） |
| F17 | 编辑锁 | 有方案 | `/governance` | 获取、释放和校验锁 | 编辑锁集成测试 | 完成 |
| F18 | 评论和实时协作 | 已登录且有方案 | `/collab` | 评论、心跳、变更、SSE 短期凭证 | `CollabApiIntegrationTest` | 完成（单实例） |
| F19 | 字段级锁 | 有方案 | `/collab` | 字段锁和写接口校验 | 字段锁集成测试 | 完成 |
| F20 | 审计链查询和校验 | 项目经理/管理员 | `/audit` | 事件查询和链校验 | 审计集成测试 | 完成（单实例） |
| F21 | 项目库和复盘 | 存在项目 | `/library` | 标签、检索、复盘 | `LibraryApiIntegrationTest` | 完成 |
| F22 | BI 看板 | 已登录 | `/dashboard` | 聚合项目、指标和预警 | `DashboardApiIntegrationTest` | 完成 |
| F23 | 规则评分和推荐 | 成功测算 | `/calculation` | 评分、推荐、运营记录 | `AiApiIntegrationTest` | 完成（规则辅助） |
| F24 | Excel 导入 | 有方案 | 输入相关入口 | 导入任务和输入写入 | `ExcelImportApiIntegrationTest` | 完成自动化基线（原子重试/千行） |

## 人工验收规则

每项功能至少按“进入页面→准备前置数据→执行操作→刷新页面→核对结果→重复或异常操作”完成一次验收。需要手工改库、刷新后丢失状态、依赖隐藏入口或无法进入下一步的功能不得标记为完成。

## 当前阻断项

1. F06 需要通过 `docs/financial_golden_cases.md` 完成独立业务复核。
2. F15 已完成中文元数据和 CJK 字体结构自动校验，正文可读性仍需固定样例版式人工验收。
3. F18 已覆盖一次性短期凭证、评论删除权限、主动离开和无心跳超时清理；真实浏览器异常断网与 SSE 重连仍需人工验收。
4. F24 已覆盖字段级错误、空文件、成功与失败重试原子性和 1000 行导入；接近上传上限和第三方复杂模板留待兼容性测试。
