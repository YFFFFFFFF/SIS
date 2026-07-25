# M1 Backend Core Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the M1 backend core loop: Project -> Scenario -> ParameterSet -> Financial Engine -> Calculation Task -> Results -> Audit.

**Architecture:** Continue the existing Spring Boot modular monolith under `backend/src/main/java/com/sis/iids`. Use controller/service/repository/model boundaries from `.ai/codeGuide/02-backend-architecture.md`, unified API responses from `.ai/codeGuide/03-api-result-error.md`, and test-first development for financial engine and service behavior. Persist M1 data using the existing Flyway schema and JPA repositories.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Spring Data JPA, Spring Validation, Spring Security foundation, Flyway, H2 dev/test database, Maven/JUnit.

---

## File Structure

- `backend/src/main/java/com/sis/iids/project/`: Project entity, repository, service, controller, DTOs.
- `backend/src/main/java/com/sis/iids/scenario/`: Scenario, ParameterSet, investment and financing entities, repositories, service, controller, DTOs.
- `backend/src/main/java/com/sis/iids/engine/financial/`: Financial engine implementation and result objects.
- `backend/src/main/java/com/sis/iids/calculation/`: Calculation task entity, repository, service, controller, task status DTOs.
- `backend/src/main/java/com/sis/iids/audit/`: Audit event entity, repository, service.
- `backend/src/test/java/com/sis/iids/`: Unit and slice tests for financial engine and services.

### Task 1: Financial Engine Red-Green

**Files:**
- Test: `backend/src/test/java/com/sis/iids/engine/financial/FinancialEngineTest.java`
- Create: `backend/src/main/java/com/sis/iids/engine/financial/FinancialEngine.java`
- Create: `backend/src/main/java/com/sis/iids/engine/financial/FinancialResult.java`

- [ ] Step 1: Write failing tests for total investment, cash flow rows, NPV, ROI, static payback, and dynamic payback using a standard sample.
- [ ] Step 2: Run `mvn -q test -Dtest=FinancialEngineTest` and verify tests fail because `FinancialEngine` is missing.
- [ ] Step 3: Implement minimal financial engine logic.
- [ ] Step 4: Run `mvn -q test -Dtest=FinancialEngineTest` and verify pass.

### Task 2: Project Service And API

**Files:**
- Create: `backend/src/main/java/com/sis/iids/project/*`
- Test: `backend/src/test/java/com/sis/iids/project/ProjectServiceTest.java`

- [ ] Step 1: Write failing service test for creating and listing projects.
- [ ] Step 2: Run test and verify fail because service/repository classes are missing.
- [ ] Step 3: Implement Project entity, repository, DTO, service, and controller.
- [ ] Step 4: Run `mvn -q test -Dtest=ProjectServiceTest` and verify pass.

### Task 3: Scenario And ParameterSet Service

**Files:**
- Create: `backend/src/main/java/com/sis/iids/scenario/*`
- Test: `backend/src/test/java/com/sis/iids/scenario/ScenarioServiceTest.java`

- [ ] Step 1: Write failing service test for creating a scenario and saving parameters.
- [ ] Step 2: Run test and verify fail because scenario service is missing.
- [ ] Step 3: Implement Scenario, ParameterSet, InvestmentItem, FinancingPlan entities/repositories/DTOs/service/controller.
- [ ] Step 4: Run `mvn -q test -Dtest=ScenarioServiceTest` and verify pass.

### Task 4: Calculation Task Core Loop

**Files:**
- Create: `backend/src/main/java/com/sis/iids/calculation/*`
- Test: `backend/src/test/java/com/sis/iids/calculation/CalculationServiceTest.java`

- [ ] Step 1: Write failing test that creates a calculation task for a scenario and stores result metrics/cash flows.
- [ ] Step 2: Run test and verify fail because calculation service is missing.
- [ ] Step 3: Implement CalculationTask, CashFlowRow, CalculationResult entities/repositories/service/controller.
- [ ] Step 4: Run `mvn -q test -Dtest=CalculationServiceTest` and verify pass.

### Task 5: Audit Event Hook

**Files:**
- Create: `backend/src/main/java/com/sis/iids/audit/*`
- Modify: project/scenario/calculation services to record audit events.
- Test: `backend/src/test/java/com/sis/iids/audit/AuditServiceTest.java`

- [ ] Step 1: Write failing test for recording an audit event during project creation or calculation.
- [ ] Step 2: Run test and verify fail because audit service is missing.
- [ ] Step 3: Implement AuditEvent entity/repository/service and call it from core services.
- [ ] Step 4: Run audit and affected service tests.

### Task 6: Backend Verification

**Files:**
- Update: `spec/intelligent-investment-decision-support/test_plan.md` and `handoff.md` if needed.

- [ ] Step 1: Run `mvn -q test`.
- [ ] Step 2: Run `mvn -q -DskipTests package`.
- [ ] Step 3: Record results in handoff or dev notes.
- [ ] Step 4: Commit implementation.

## Self-Review

- Spec coverage: Covers M1 backend core loop only, matching the approved option 1.
- Placeholder scan: No unresolved placeholders; tests and commands are explicit.
- Type consistency: Package names match the existing `com.sis.iids` baseline.
