# 用户测试环境说明

本文档用于本地开发和用户系统测试。默认使用 `dev` profile、H2 内存数据库和仓库内置测试账号，不适用于生产环境。

## 1. 前置依赖检查

在仓库根目录执行：

```powershell
.\scripts\check_test_environment.ps1
```

最低要求：Java 17、Maven 3、Node.js 22、npm 10。Playwright E2E 还需要本机安装 Google Chrome。脚本同时检查项目文件以及 `8080`、`5173` 端口占用情况；端口已被正在运行的本项目服务占用时可以忽略警告。

## 2. 启动与健康检查

终端一：

```powershell
cd E:\SIS\backend
mvn spring-boot:run
```

终端二：

```powershell
cd E:\SIS\frontend
npm install
npm run dev
```

访问地址：

- 前端：`http://localhost:5173`
- 后端 Swagger：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI：`http://localhost:8080/v3/api-docs`

后端健康检查可使用：

```powershell
Invoke-WebRequest http://localhost:8080/swagger-ui/index.html -UseBasicParsing
```

返回 HTTP 200 表示后端已可访问；打开前端登录页表示前端和代理已就绪。

## 3. 测试账号

所有内置账号的密码均为 `Password123!`。

| 用户名 | 角色 | 主要验证范围 |
|---|---|---|
| `investment_analyst` | 投资分析师 | 项目、方案、测算和比较 |
| `finance_specialist` | 财务专员 | 财务输入与复核 |
| `technical_engineer` | 技术工程师 | 投资项维护 |
| `project_manager` | 项目经理 | 审批和项目管理 |
| `admin` | 管理员 | 全功能与审计验证 |
| `analyst` | 兼容投资分析师 | 兼容旧测试脚本 |

## 4. 测试数据初始化

H2 数据库在后端进程启动时由 Flyway 自动初始化，包含测试账号。后端启动后，可在仓库根目录执行幂等初始化脚本：

```powershell
.\scripts\initialize_demo_data.ps1
```

脚本会创建 `UAT-DEMO-001`、两个已测算方案和一份基准方案 Excel 报告；如果项目编码已经存在，则安全退出且不重复写入。

也可以按以下顺序手工创建业务演示数据：

1. 使用 `investment_analyst` 创建项目和测算方案。
2. 录入参数、投资项和融资方案，或使用 Excel 模板导入。
3. 执行财务测算，检查指标和现金流。
4. 执行风险分析、方案比较和组合优化。
5. 生成 Excel/PDF 报告。
6. 使用财务专员和项目经理完成固定审批流。

Playwright 基线用例会自动创建带时间戳的项目，并验证刷新后的持久化，不依赖手工预置项目。

## 5. 安全重置

本地 `dev` profile 使用 H2 内存数据库。安全重置步骤：

1. 停止后端进程。
2. 确认没有设置 `SPRING_PROFILES_ACTIVE=prod`，也没有覆盖 PostgreSQL 数据源。
3. 重新执行 `mvn spring-boot:run`。

后端重启后，H2 业务数据会恢复为空库，Flyway 会重新创建测试账号。不要把此方式用于 PostgreSQL、Docker Compose 或任何共享环境；这些环境的数据清理必须先备份并由环境负责人执行。

## 6. 自动化验证

```powershell
cd E:\SIS\backend
mvn test

cd E:\SIS\frontend
npm test
npm run test:e2e
npm run build
```

## 7. 常见问题

- `8080` 或 `5173` 被占用：先确认是否已有本项目服务运行，再用 `Get-NetTCPConnection -State Listen -LocalPort 8080,5173` 定位进程。
- 登录失败：确认后端已完成 Flyway 初始化，账号密码大小写完全一致。
- 前端接口失败：确认 Vite 运行在 `5173`，后端运行在 `8080`。
- E2E 启动失败：确认 Google Chrome 已安装，并先执行一次 `npm install`。
- 数据与预期不一致：仅在本地 H2 环境按“安全重置”步骤重启后端。
