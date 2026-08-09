# 智能投资测算与决策支持系统

面向投资项目全生命周期的测算与决策支持系统：项目建档、方案维护、参数录入、财务测算（NPV/IRR/回收期/三类现金流报表）、敏感性/目标反算/盈亏平衡/蒙特卡洛风险分析、多方案对比与组合优化、结构化报告（Excel/PDF）、风险预警、BPM 审批流、协同编辑、项目库复盘、AI 决策辅助与审计哈希链。

当前项目不是企业级平台，也不依赖 Docker 才能运行。由于当前电脑无法安装 Docker，推荐使用本地开发模式启动：后端 Spring Boot 连接本地 H2 内存库，前端 Vue 3 通过 Vite 代理访问后端 API。

## 文档入口

- **[升级改造总体方案](docs/upgrade_plan.md)：M1 → PRD 全量的改造范围、优先级、设计红线与进度追踪（18/18 已完成）。任何改造工作必须先读此文档。**
- [用户使用手册](docs/user_manual.md)：面向业务用户的本地启动和业务操作步骤。
- [API 概览](docs/api_overview.md)：后端接口、请求示例、响应结构、角色权限矩阵。
- Swagger UI：启动后端后访问 `http://localhost:8080/swagger-ui/index.html`。
- [测试计划](docs/test_plan.md)：自动化验证命令和人工验收清单。
- [开发说明](docs/dev_notes.md)：代码结构、配置、开发约束和已知限制。
- [交接说明](docs/handoff.md)：当前交付状态、验证记录、风险和下一步建议。

## 技术栈

后端：

- Java 17+
- Spring Boot 3.3
- Spring Security + JWT
- Spring Data JPA
- Flyway
- H2（本地开发默认）
- Apache POI（Excel 导入/导出）

前端：

- Node.js 22+
- npm 10+
- Vue 3
- TypeScript
- Vite
- Element Plus
- ECharts

## 本地启动

### 1. 启动后端

```powershell
cd E:\SIS\backend
mvn -q test
mvn spring-boot:run
```

后端默认地址：

- API 根路径：`http://localhost:8080/api/v1`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

### 2. 启动前端

```powershell
cd E:\SIS\frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

Vite 开发服务器会把 `/api` 请求代理到 `http://localhost:8080`。

## 本地测试账号

后端 Flyway 种子数据会创建以下账号，默认密码均为 `Password123!`。

| 用户名 | 角色 | 中文说明 |
| --- | --- | --- |
| `analyst` | `ANALYST`, `INVESTMENT_ANALYST` | 兼容旧账号的投资分析师 |
| `investment_analyst` | `INVESTMENT_ANALYST` | 投资分析师 |
| `finance_specialist` | `FINANCE_SPECIALIST` | 财务专员 |
| `technical_engineer` | `TECHNICAL_ENGINEER` | 技术工程师 |
| `project_manager` | `PROJECT_MANAGER` | 项目管理者 |
| `admin` | `ADMIN`, `SYSTEM_ADMINISTRATOR` | 管理员/系统管理员 |

推荐优先使用：

```text
用户名：investment_analyst
密码：Password123!
```

## M1 已实现能力

后端已实现：

- JWT 登录认证
- 固定 M1 角色权限矩阵
- 项目创建、更新、查询
- 测算方案创建、更新、查询
- 测算参数维护
- 投资项和融资方案录入
- 财务测算任务创建与执行
- 测算指标和现金流结果持久化
- Excel 模板导入
- Excel 报表生成和下载
- 固定审批链：提交、财务复核、项目经理审批、驳回
- 测算方案编辑锁
- 审计日志记录和查询
- 中文错误消息、中文 OpenAPI 元信息、中文 Excel 报表标题和表头

前端已实现：

- 中文登录页
- 中文投资测算工作台
- 项目和测算方案管理
- 测算参数、投资项、融资方案录入
- 测算执行、指标表格、现金流表格、指标柱状图
- 报表生成和下载入口
- 审批、编辑锁和审计查询入口
- 状态、角色、指标、审批节点、审计动作的中文展示映射

## 权限与流程约束

当前版本采用固定权限矩阵，不提供权限配置后台。

核心规则：

- 投资分析师和项目管理者可以维护项目和方案。
- 财务专员可以维护财务参数并进行财务复核。
- 技术工程师可以维护投资项，但不能修改财务参数。
- 项目管理者负责最终审批和审计查询。
- 管理员拥有全部 M1 权限。
- 已提交或已审批通过的测算方案在工作台上会进入只读状态。
- 其他用户持有编辑锁时，当前用户不能继续编辑该方案。

## 验证命令

后端：

```powershell
cd E:\SIS\backend
mvn -q test
```

前端：

```powershell
cd E:\SIS\frontend
npm run build
```

## Docker 状态

仓库保留了 `docker-compose.yml` 和 `.env.example`，但当前电脑无法安装 Docker，因此本阶段以本地开发模式为准。

后续如果环境允许，可以再执行：

```powershell
cd E:\SIS
Copy-Item .env.example .env
docker compose config
docker compose up --build
```

非个人本地环境使用前，必须替换数据库密码和 `IIDS_SECURITY_JWT_SECRET`。

## 当前已知限制

- 仍是 M1 原型闭环，不是完整企业级系统。
- 权限矩阵固定在代码中，暂不支持页面配置。
- 测算模型为 M1 基础版，复杂行业模型、敏感性分析和多版本对比仍待扩展。
- 前端目前没有独立单元测试配置，主要通过 `npm run build` 做类型和构建验证。
- Docker 暂不可用时，请使用本地后端和前端开发服务器运行。