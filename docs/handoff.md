# M1 移交说明

本文档记录当前 M1 交付范围、验证状态、运行方式和后续接手建议。

## 1. 当前版本状态

当前仓库已经完成 M1 主流程闭环：

1. 登录和 RBAC 基础能力。
2. 项目管理。
3. 测算方案管理。
4. 参数、投资项、融资方案录入。
5. 异步财务测算任务。
6. 测算指标和现金流持久化。
7. Excel 导入任务。
8. Excel 报告生成和下载。
9. 固定审批链。
10. 场景编辑锁。
11. 审计事件查询。
12. Vue 工作台前端。
13. Docker Compose 部署基线。

## 2. 关键入口

| 内容 | 路径 |
| --- | --- |
| 项目 README | `README.md` |
| 用户手册 | `docs/user_manual.md` |
| API 总览 | `docs/api_overview.md` |
| 测试计划 | `docs/test_plan.md` |
| 开发说明 | `docs/dev_notes.md` |
| 后端入口 | `backend/src/main/java/com/sis/iids/IidsApplication.java` |
| 前端入口 | `frontend/src/main.ts` |
| 工作台页面 | `frontend/src/views/WorkbenchHome.vue` |
| Compose 文件 | `docker-compose.yml` |

## 3. 启动方式

本地开发：

```powershell
cd E:\SIS\backend
mvn spring-boot:run
```

```powershell
cd E:\SIS\frontend
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

默认账号：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `analyst` | `Password123!` | `ANALYST` |
| `admin` | `Password123!` | `ADMIN` |

## 4. 部署基线

Compose 服务：

- `postgres`
- `redis`
- `backend`
- `frontend`

启动前：

```powershell
cd E:\SIS
Copy-Item .env.example .env
```

必须替换：

- `POSTGRES_PASSWORD`
- `IIDS_SECURITY_JWT_SECRET`

启动：

```powershell
docker compose up --build
```

注意：

- 当前机器未安装或未暴露 `docker` 命令，因此 Compose 未在当前机器完成实跑验证。
- 后端 Compose profile 默认为 `prod`，数据源由环境变量覆盖到 PostgreSQL。
- 报告和上传目录通过 `backend-data` volume 持久化。

## 5. 当前验证状态

2026-07-27 在主仓库已执行：

| 命令 | 结果 | 备注 |
| --- | --- | --- |
| `mvn -q test` | 通过 | 后端测试退出码 0 |
| `mvn -q -DskipTests package` | 通过 | 后端打包退出码 0 |
| `npm run build` | 通过 | 前端构建退出码 0，有 Vite 警告 |
| `docker compose config` | 未通过环境检查 | `docker` 命令不可识别 |

前端构建警告：

- `@vueuse/core` 注释被 Rollup 移除。
- 主 chunk 超过 500 kB。

npm 安全审计：

- `npm install` 报告 5 个漏洞。
- 未执行 `npm audit fix --force`，因为该命令可能引入破坏性升级。

## 6. 已知风险

- Compose 文件语法尚未在当前机器用 Docker 实际验证。
- PostgreSQL 真实运行链路尚未做自动化集成测试。
- 前端缺少 E2E 测试。
- Excel 导入只覆盖 M1 基线场景，复杂模板兼容性需要继续补充。
- 审批和锁能力已在后端存在，但前端没有完整业务态联动禁用。
- 项目中尚无自动生成 OpenAPI 文档。

## 7. 推荐接手顺序

1. 安装 Docker Desktop，补跑 `docker compose config` 和 `docker compose up --build`。
2. 用浏览器按 `docs/user_manual.md` 完成一次端到端验收。
3. 为 PostgreSQL 模式补一组集成测试。
4. 为前端补 Playwright E2E：登录、建项目、建方案、测算、报告下载。
5. 引入 OpenAPI 文档生成。
6. 拆分 `WorkbenchHome.vue`，降低单文件复杂度。
7. 处理 npm audit 项，先评估依赖升级影响，不直接强制修复。

## 8. Git 状态

本轮文档任务应作为单独提交合入主线，建议提交信息：

```text
docs: add m1 handoff documentation
```
