# -*- coding: utf-8 -*-
from pathlib import Path
import json
from datetime import datetime, timezone

spec_dir = Path(r"E:/projects/ai-coding-workflow/spec/intelligent-investment-decision-support")
mem_dir = Path(r"E:/projects/ai-coding-workflow/.ai/memory/intelligent-investment-decision-support")
mem_dir.mkdir(parents=True, exist_ok=True)

tasks = """# Tasks: intelligent-investment-decision-support

## Status

Approved (M1 MVP)

## Active Scope

M1 only: project library, auth/RBAC, scenario/parameters, financial calculation P0,
async one-click calc, Excel import, fixed approval chain, edit lock, audit log,
standard samples, Vue frontend shell, Docker Compose.

## Task 1: Repository scaffold and deploy baseline

**Purpose**

Create monorepo layout under E:/SIS with backend, frontend, docker-compose, and root README.

**Dependencies**

- Approved design.md M1 section

**Files**

- Create: README.md, docker-compose.yml, .gitignore
- Create: backend/pom.xml, backend/src/main/resources/application.yml
- Create: frontend/package.json, frontend/vite.config.ts, frontend/index.html

**Verification**

    ls E:/SIS

**Done When**

- [ ] Project roots exist and are buildable skeleton

## Task 2: Database schema and common backend foundation

**Purpose**

Flyway migrations + common API result/error/security/task base.

**Dependencies**

- Task 1

**Files**

- Create: backend/src/main/resources/db/migration/V1__init_schema.sql
- Create: backend common/security packages

**Verification**

    cd backend && mvn -q -DskipTests package

**Done When**

- [ ] Schema covers M1 tables
- [ ] Unified API response and auth skeleton compile

## Task 3: Auth, RBAC, project and scenario APIs

**Purpose**

Login, roles, project CRUD, scenario/parameter APIs, audit hooks.

**Dependencies**

- Task 2

**Verification**

    cd backend && mvn -q test

**Done When**

- [ ] Login returns JWT
- [ ] CRUD project/scenario works with role checks

## Task 4: Financial engine and standard samples

**Purpose**

Implement NPV/IRR/payback/ROI/cashflow tables and sample regression tests
(deviation <= 1 per mille).

**Dependencies**

- Task 2

**Verification**

    cd backend && mvn -q test -Dtest=FinancialEngineTest

**Done When**

- [ ] Core metrics pass standard sample assertions

## Task 5: Calculation task, report, excel import, approval, edit lock

**Purpose**

Async one-click calculation, report export, excel import, fixed approval chain,
scenario edit lock.

**Dependencies**

- Task 3, Task 4

**Verification**

    cd backend && mvn -q test

**Done When**

- [ ] Calculate task transitions PENDING to SUCCESS
- [ ] Import/approval/lock endpoints available

## Task 6: Frontend M1 workbench

**Purpose**

Login, project list, scenario edit, trigger calculate, view results/report entry.

**Dependencies**

- Task 5 APIs

**Verification**

    cd frontend && npm install && npm run build

**Done When**

- [ ] Frontend production build succeeds

## Task 7: End-to-end verification and handoff

**Purpose**

Run backend tests, frontend build, update test_plan/dev_notes/handoff.

**Dependencies**

- Task 1-6

**Verification**

    cd backend && mvn -q test
    cd frontend && npm run build

**Done When**

- [ ] Commands recorded in handoff.md
- [ ] M1 acceptance mapped in test_plan.md

## Quality Checklist

- [x] Tasks cover the approved M1 design
- [x] Each task names files and verification
- [x] Dependencies are explicit
"""

(spec_dir / "tasks.md").write_text(tasks, encoding="utf-8")

now = datetime.now(timezone.utc).isoformat()
records = [
    {
        "timestamp": now,
        "feature": "intelligent-investment-decision-support",
        "step": "planner",
        "decision": "approve",
        "reason": "M1 scope confirmed by user",
    },
    {
        "timestamp": now,
        "feature": "intelligent-investment-decision-support",
        "step": "architect",
        "decision": "approve",
        "reason": "M1 modular monolith design accepted",
    },
    {
        "timestamp": now,
        "feature": "intelligent-investment-decision-support",
        "step": "task_splitter",
        "decision": "approve",
        "reason": "M1 tasks approved",
    },
]
with (mem_dir / "gates.jsonl").open("w", encoding="utf-8") as f:
    for r in records:
        f.write(json.dumps(r, ensure_ascii=False) + "\n")
(mem_dir / "current_step.txt").write_text("developer", encoding="utf-8")
print("workflow artifacts updated")
