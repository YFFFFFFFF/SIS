# Chinese Native Localization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the M1 investment decision support system feel natural for Chinese-native users without changing API field names or enum contracts.

**Architecture:** Keep backend/API machine values stable and add Chinese presentation at product boundaries: frontend UI labels, user-facing backend messages, exported report labels, OpenAPI metadata, and docs. Avoid deep domain refactors and avoid changing enum values stored in the database.

**Tech Stack:** Spring Boot 3, JUnit/MockMvc, Apache POI, Vue 3, Element Plus, TypeScript, Vite.

---

### Task 1: Backend Chinese User-Facing Contract

**Files:**
- Modify: `backend/src/test/java/com/sis/iids/report/ReportApiIntegrationTest.java`
- Modify: `backend/src/test/java/com/sis/iids/project/ProjectApiIntegrationTest.java`
- Modify: `backend/src/main/java/com/sis/iids/report/ReportService.java`
- Modify: backend service/error files that expose English business messages.

- [ ] **Step 1: Write failing tests**

Assert generated Excel sheets use `指标汇总` and `现金流量表`, report title starts with `投资收益测算报告`, and not-found API messages are Chinese.

- [ ] **Step 2: Run targeted tests and confirm RED**

Run: `mvn -q -Dtest=ReportApiIntegrationTest,ProjectApiIntegrationTest test`
Expected: FAIL on English sheet/message assertions before implementation.

- [ ] **Step 3: Implement minimal backend localization**

Change business exception messages, report title, workbook sheet names and headers to Chinese. Keep enum values and JSON field names unchanged.

- [ ] **Step 4: Run targeted tests and full backend tests**

Run targeted tests, then `mvn -q test`.

### Task 2: Frontend Chinese Presentation

**Files:**
- Create: `frontend/src/shared/i18n/display.ts`
- Modify: `frontend/src/views/LoginView.vue`
- Modify: `frontend/src/layouts/WorkbenchLayout.vue`
- Modify: `frontend/src/views/WorkbenchHome.vue`
- Modify: `frontend/src/components/MetricChart.vue`
- Modify: `frontend/index.html`

- [ ] **Step 1: Add centralized display dictionaries**

Create maps for status, role, metric, project type, action labels, and helper functions.

- [ ] **Step 2: Replace visible English UI copy**

Translate login, navigation, tabs, cards, forms, table columns, buttons, alerts, empty states, and success/error messages.

- [ ] **Step 3: Preserve API contract values**

Continue submitting original enum values such as `DRAFT`, `SUCCESS`, `INVESTMENT_ANALYST`, and metric codes.

- [ ] **Step 4: Verify frontend build**

Run: `npm run build`.

### Task 3: Chinese Documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/api_overview.md`

- [ ] **Step 1: Rewrite README in Chinese**

Document local startup without Docker, default accounts, core modules, verification commands, and known limitations.

- [ ] **Step 2: Rewrite API overview in Chinese**

Fix existing table formatting issue and describe API groups, role permissions, status values, and examples in Chinese.

- [ ] **Step 3: Final verification**

Run frontend build and backend tests after docs changes if code changed since the previous run.
