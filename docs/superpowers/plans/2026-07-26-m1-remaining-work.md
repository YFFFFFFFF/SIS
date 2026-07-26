# M1 Remaining Work Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the remaining M1 MVP scope for the Intelligent Investment Decision Support System after the backend core calculation loop.

**Architecture:** Continue the modular monolith in `E:\SIS\backend` and add a Vue 3 workbench in `E:\SIS\frontend`. Keep backend modules isolated by business boundary: auth, report, importx, approval, collaboration, calculation, audit. Use TDD per task and commit each completed slice.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Spring Security, Spring Data JPA, Flyway, H2/PostgreSQL, Vue 3, TypeScript, Vite, Pinia, Vue Router, Element Plus, ECharts, Docker Compose.

---

## Current Completed Baseline

Completed on `master`:

- Backend project/scenario/parameter APIs.
- Financial engine for total investment, NPV, ROI, static payback, dynamic payback and cash-flow rows.
- Investment item and financing plan APIs.
- Calculation task synchronous core loop with persisted metrics and cash-flow rows.
- Calculation completion audit event and audit query API.
- Backend tests and package currently pass from `E:\SIS\backend`:
  - `mvn -q test`
  - `mvn -q -DskipTests package`

Known partial/gaps against approved M1:

- Auth/RBAC is not implemented; tests disable filters and Spring still uses generated default password.
- Project/scenario update/status/data-permission behavior is incomplete.
- Financial engine lacks IRR and capital net profit rate; cash-flow statements are simplified.
- Calculation is synchronous, not PENDING/RUNNING/SUCCESS worker-driven.
- Excel import, report export, approval chain and edit lock are absent.
- Frontend is effectively empty; no Vite app, routes, API client or pages.
- Root `README.md` and `docker-compose.yml` are absent.
- Handoff/test plan documents are not updated for M1 acceptance.

---

## Recommended Execution Order

1. Backend hardening first: auth/RBAC, error/status normalization, missing financial metrics.
2. M1 business completion: async calculation worker, Excel import, report export, approval, edit lock.
3. Frontend workbench: login, project/scenario workflow, calculation results, audit/report/approval entry points.
4. Deployment and handoff: Docker Compose, README, test plan, acceptance mapping.

This order prevents the frontend from building against unstable contracts.

---

### Task 1: Auth And RBAC Foundation

**Purpose:** Replace generated Spring security behavior with real M1 login, JWT, roles and backend access control.

**Files:**

- Create: `backend/src/main/java/com/sis/iids/auth/*`
- Create: `backend/src/main/java/com/sis/iids/security/*`
- Create: `backend/src/test/java/com/sis/iids/auth/AuthApiIntegrationTest.java`
- Create: `backend/src/test/java/com/sis/iids/security/RbacIntegrationTest.java`
- Modify: `backend/src/main/java/com/sis/iids/common/error/ErrorCode.java`
- Modify: `backend/src/main/java/com/sis/iids/common/error/GlobalExceptionHandler.java`
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql` only if still pre-release; otherwise add `V2__auth_seed.sql`

**Steps:**

- [ ] Write RED test: `POST /api/v1/auth/login` returns JWT for seeded analyst user.
- [ ] Write RED test: anonymous request to protected project API returns `401`.
- [ ] Write RED test: role without permission receives `403` for admin-only endpoint.
- [ ] Implement `SysUser`, `SysRole`, repositories, password encoder, JWT service, auth controller.
- [ ] Implement `SecurityConfig`, JWT filter, `UserDetailsService` and method security.
- [ ] Seed M1 users/roles for local dev or test profile.
- [ ] Add audit events for login success/failure.
- [ ] Run `mvn -q test -Dtest=AuthApiIntegrationTest,RbacIntegrationTest`.
- [ ] Run `mvn -q test`.
- [ ] Commit: `feat: add auth rbac foundation`.

**Acceptance:**

- Login returns token.
- Protected APIs reject anonymous users.
- Role checks are enforced server-side.
- Existing integration tests run with authenticated requests instead of `addFilters = false` where practical.

---

### Task 2: Project And Scenario Completion

**Purpose:** Complete M1 project/scenario CRUD surface and audit important mutations.

**Files:**

- Modify: `backend/src/main/java/com/sis/iids/project/*`
- Modify: `backend/src/main/java/com/sis/iids/scenario/*`
- Modify: `backend/src/test/java/com/sis/iids/project/ProjectApiIntegrationTest.java`
- Modify: `backend/src/test/java/com/sis/iids/scenario/ScenarioParameterApiIntegrationTest.java`

**Steps:**

- [ ] Write RED test for project update: name, status, department, tags, description.
- [ ] Write RED test for scenario update/status transition.
- [ ] Write RED test that project/scenario mutation writes `AuditEvent`.
- [ ] Implement `PUT /api/v1/projects/{id}`.
- [ ] Implement `PUT /api/v1/scenarios/{id}`.
- [ ] Add audit hooks for create/update/status changes.
- [ ] Run focused project/scenario tests.
- [ ] Run `mvn -q test`.
- [ ] Commit: `feat: complete project scenario management`.

**Acceptance:**

- Project档案支持 M1 所需 CRUD/update/status/tag.
- Scenario 可维护核心字段和状态.
- 关键写操作可在审计日志中查询.

---

### Task 3: Financial Engine P0 Completion

**Purpose:** Fill the remaining M1 financial indicators and regression samples.

**Files:**

- Modify: `backend/src/main/java/com/sis/iids/engine/financial/*`
- Modify: `backend/src/main/java/com/sis/iids/calculation/*`
- Create: `backend/src/test/resources/samples/sample-industrial-plant.json`
- Optional create: `backend/src/test/resources/samples/sample-infra-road.json`
- Modify: `backend/src/test/java/com/sis/iids/engine/financial/FinancialEngineTest.java`
- Modify: `backend/src/test/java/com/sis/iids/calculation/CalculationApiIntegrationTest.java`

**Steps:**

- [ ] Write RED test for IRR on the current standard sample.
- [ ] Write RED test for capital net profit rate.
- [ ] Write RED test for formula version, parameterSetId and inputHash persistence.
- [ ] Implement IRR with bounded iteration and deterministic failure handling.
- [ ] Implement capital net profit rate using equity/capital assumptions.
- [ ] Persist new metrics through calculation results.
- [ ] Add sample JSON regression fixture.
- [ ] Run `mvn -q test -Dtest=FinancialEngineTest,CalculationApiIntegrationTest`.
- [ ] Run `mvn -q test`.
- [ ] Commit: `feat: complete financial p0 metrics`.

**Acceptance:**

- FR-01-03 P0 metrics include NPV, IRR, payback, ROI and capital net profit rate.
- Standard sample deviation remains within 1‰.
- Result rows keep formula version, engine version, parameter set and input hash traceability.

---

### Task 4: Async Calculation Worker

**Purpose:** Convert calculation from synchronous execution to M1 task lifecycle with worker execution.

**Files:**

- Modify: `backend/src/main/java/com/sis/iids/calculation/*`
- Create: `backend/src/main/java/com/sis/iids/worker/CalculationWorker.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/java/com/sis/iids/calculation/CalculationApiIntegrationTest.java`

**Steps:**

- [ ] Write RED test: task creation returns `PENDING` and `taskId` immediately.
- [ ] Write RED test: worker moves task `PENDING -> RUNNING -> SUCCESS` and persists results.
- [ ] Write RED test: failed task stores error message and can be queried.
- [ ] Split `CalculationService` into task creation and execution methods.
- [ ] Implement `CalculationWorker` gated by `iids.worker.enabled`.
- [ ] Add request key/idempotency handling for repeated calculate requests.
- [ ] Normalize status names to the design: `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`.
- [ ] Run focused calculation tests.
- [ ] Run `mvn -q test`.
- [ ] Commit: `feat: run calculation tasks asynchronously`.

**Acceptance:**

- One-click calculation is task-based.
- API can query progress and final result.
- Failure state is inspectable and auditable.

---

### Task 5: Excel Import Template

**Purpose:** Support M1 Excel template import for scenario basic data.

**Files:**

- Create: `backend/src/main/java/com/sis/iids/importx/*`
- Create: `backend/src/test/java/com/sis/iids/importx/ExcelImportApiIntegrationTest.java`
- Create: `backend/src/test/resources/import/sample-m1-template.xlsx`
- Modify: `backend/src/main/resources/application.yml`

**Steps:**

- [ ] Write RED test: upload valid Excel template creates/updates investment items, financing plans and parameter set.
- [ ] Write RED test: invalid template returns field-level validation errors and creates failed import job.
- [ ] Implement upload endpoint: `POST /api/v1/scenarios/{id}/import/excel`.
- [ ] Implement import job persistence and status query.
- [ ] Parse template using Apache POI.
- [ ] Add size/type/template validation.
- [ ] Add audit event for import success/failure.
- [ ] Run import focused tests.
- [ ] Run `mvn -q test`.
- [ ] Commit: `feat: add excel template import`.

**Acceptance:**

- Excel 模板导入可用.
- 错误可定位到 sheet/row/field.
- 导入行为可审计.

---

### Task 6: Report Export

**Purpose:** Generate M1 standardized investment return report entry and downloadable export.

**Files:**

- Create: `backend/src/main/java/com/sis/iids/report/*`
- Create: `backend/src/test/java/com/sis/iids/report/ReportApiIntegrationTest.java`
- Modify: `backend/src/main/resources/application.yml`

**Steps:**

- [ ] Write RED test: completed calculation can generate report document.
- [ ] Write RED test: report download returns non-empty Excel file.
- [ ] Optional RED test: PDF summary endpoint returns a document placeholder if PDF library is deferred.
- [ ] Implement report generation from persisted metrics and cash-flow rows.
- [ ] Store report metadata in `report_document`.
- [ ] Save files under configured `iids.report-dir`.
- [ ] Add audit event for report generation/download.
- [ ] Run report focused tests.
- [ ] Run `mvn -q test`.
- [ ] Commit: `feat: add m1 report export`.

**Acceptance:**

- FR-01-06 has a report artifact path.
- Excel export includes metric summary and cash-flow table.
- Report generation failure does not delete calculation results.

---

### Task 7: Approval Chain

**Purpose:** Implement fixed M1 approval chain: submit -> review -> approve.

**Files:**

- Create: `backend/src/main/java/com/sis/iids/approval/*`
- Create: `backend/src/test/java/com/sis/iids/approval/ApprovalApiIntegrationTest.java`
- Modify: `backend/src/main/resources/db/migration/*` if schema gap requires additive migration.

**Steps:**

- [ ] Write RED test: submit scenario creates approval instance at review node.
- [ ] Write RED test: reviewer approves and moves to approval node.
- [ ] Write RED test: approver approves and instance becomes approved.
- [ ] Write RED test: reject records decision and terminal status.
- [ ] Implement approval instance and record repositories.
- [ ] Implement approval service state machine.
- [ ] Add role checks per node.
- [ ] Add audit events for submit/review/approve/reject.
- [ ] Run approval tests.
- [ ] Run `mvn -q test`.
- [ ] Commit: `feat: add fixed approval chain`.

**Acceptance:**

- 固定审批链可跑通.
- 节点操作有角色限制.
- 审批记录和审计日志完整.

---

### Task 8: Edit Lock

**Purpose:** Add non-real-time collaboration lock for scenario editing.

**Files:**

- Create: `backend/src/main/java/com/sis/iids/collaboration/*`
- Create: `backend/src/test/java/com/sis/iids/collaboration/EditLockApiIntegrationTest.java`

**Steps:**

- [ ] Write RED test: user can acquire lock for scenario.
- [ ] Write RED test: second user cannot acquire active lock.
- [ ] Write RED test: holder can release lock.
- [ ] Write RED test: expired lock can be replaced.
- [ ] Implement `POST /api/v1/scenarios/{id}/lock`.
- [ ] Implement `DELETE /api/v1/scenarios/{id}/lock`.
- [ ] Add holder and expiry validation.
- [ ] Add audit event for acquire/release/expire-replace.
- [ ] Run lock tests.
- [ ] Run `mvn -q test`.
- [ ] Commit: `feat: add scenario edit lock`.

**Acceptance:**

- 首期协同锁定编辑可用.
- 锁有 holder、holderName、expireAt.
- 锁冲突返回明确业务错误.

---

### Task 9: Frontend Scaffold And API Client

**Purpose:** Create the Vue 3 frontend foundation and connect to backend API shape.

**Files:**

- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/index.html`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/shared/api/http.ts`
- Create: `frontend/src/shared/types/*`
- Create: `frontend/src/stores/auth.ts`
- Create: `frontend/src/styles/*`

**Steps:**

- [ ] Create Vite Vue TypeScript scaffold.
- [ ] Add Element Plus, Pinia, Vue Router, Axios and ECharts dependencies.
- [ ] Implement shared API client with token injection and unified response unwrap.
- [ ] Implement login store and route guard.
- [ ] Add base layout with sidebar/topbar and loading/error states.
- [ ] Run `npm install`.
- [ ] Run `npm run build`.
- [ ] Commit: `feat: scaffold frontend workbench`.

**Acceptance:**

- Frontend production build succeeds.
- API client aligns with `{ code, message, data }`.
- Auth token is stored and attached consistently.

---

### Task 10: Frontend M1 Workbench

**Purpose:** Implement user-facing M1 flow from login to calculation result viewing.

**Files:**

- Create: `frontend/src/features/auth/*`
- Create: `frontend/src/features/project-workbench/*`
- Create: `frontend/src/features/scenario-calculation/*`
- Create: `frontend/src/features/approval-center/*`
- Create: `frontend/src/features/admin-audit/*`
- Modify: `frontend/src/router/index.ts`

**Steps:**

- [ ] Implement login page.
- [ ] Implement project list/create/edit page.
- [ ] Implement scenario list/create/edit page.
- [ ] Implement parameter, investment item and financing plan forms.
- [ ] Implement calculate button, task polling and result summary.
- [ ] Implement cash-flow table and basic chart using same result data.
- [ ] Implement report generation/download entry.
- [ ] Implement approval center basic page.
- [ ] Implement audit event query page.
- [ ] Run `npm run build`.
- [ ] Commit: `feat: add frontend m1 workflow`.

**Acceptance:**

- A user can complete the M1 happy path in browser.
- Results table and chart use the same backend data.
- Loading, empty, error and disabled states are present for core pages.

---

### Task 11: Deployment Baseline

**Purpose:** Add private deployment baseline for app, PostgreSQL and Redis.

**Files:**

- Create: `README.md`
- Create: `docker-compose.yml`
- Create: `backend/Dockerfile`
- Create: `frontend/Dockerfile`
- Create: `frontend/nginx.conf`
- Modify: `.gitignore`
- Optional create: `.env.example`

**Steps:**

- [ ] Add backend Dockerfile.
- [ ] Add frontend Dockerfile and nginx config.
- [ ] Add docker compose services: backend, frontend, postgres, redis.
- [ ] Add environment variables for DB/JWT/report/upload dirs.
- [ ] Add README quickstart for local dev and compose deployment.
- [ ] Run `mvn -q test`.
- [ ] Run `npm run build`.
- [ ] Run `docker compose config`.
- [ ] Commit: `chore: add deployment baseline`.

**Acceptance:**

- Docker Compose config validates.
- README can bootstrap a new local run.
- Sensitive defaults are documented as dev-only.

---

### Task 12: M1 Verification And Handoff

**Purpose:** Produce final M1 acceptance evidence and handoff documents.

**Files:**

- Create: `docs/test_plan.md`
- Create: `docs/dev_notes.md`
- Create: `docs/handoff.md`
- Create: `docs/api_overview.md`
- Modify: `README.md`
- Optional modify: `E:\projects\ai-coding-workflow\spec\intelligent-investment-decision-support\tasks.md` after user confirmation only.

**Steps:**

- [ ] Map M1 acceptance criteria to implemented tests and manual checks.
- [ ] Record backend test output: `mvn -q test`.
- [ ] Record backend package output: `mvn -q -DskipTests package`.
- [ ] Record frontend build output: `npm run build`.
- [ ] Record Docker validation output: `docker compose config`.
- [ ] Document known limitations and deferred M2+ items.
- [ ] Document API overview and happy-path operation steps.
- [ ] Commit: `docs: add m1 verification handoff`.

**Acceptance:**

- Handoff gives enough context for the next developer/user session.
- Test plan maps directly to SPEC M1 acceptance.
- Deferred items are explicit and not hidden as complete.

---

## Milestone Gates

### Gate A: Backend M1 Complete

Tasks 1-8 complete.

Verification:

```powershell
cd E:\SIS\backend
mvn -q test
mvn -q -DskipTests package
```

### Gate B: Frontend M1 Complete

Tasks 9-10 complete.

Verification:

```powershell
cd E:\SIS\frontend
npm install
npm run build
```

### Gate C: Delivery Baseline Complete

Tasks 11-12 complete.

Verification:

```powershell
cd E:\SIS
mvn -q -f backend\pom.xml test
npm --prefix frontend run build
docker compose config
```

---

## Suggested Next Task

Start with **Task 1: Auth And RBAC Foundation**.

Reason: It affects almost every remaining backend and frontend flow. If it is delayed, later API tests and frontend pages will be built around insecure temporary assumptions and need rework.