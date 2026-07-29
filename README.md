# Intelligent Investment Decision Support System

Personal M1 implementation of the Intelligent Investment Decision Support System. The project contains a Spring Boot backend and a Vue 3 workbench frontend.

## Documentation

- [User Manual](docs/user_manual.md): detailed local startup and business operation steps.
- [API Overview](docs/api_overview.md): M1 API endpoints, request bodies and response rules.
- Swagger UI: run the backend and open `http://localhost:8080/swagger-ui/index.html`.
- [Test Plan](docs/test_plan.md): automated verification commands and manual acceptance checklist.
- [Developer Notes](docs/dev_notes.md): code structure, conventions, configuration and known limits.
- [Handoff](docs/handoff.md): delivery status, verification record, risks and next steps.

## Local Development

Prerequisites:

- Java 17+
- Maven 3.9+
- Node.js 22+
- npm 10+

Backend:

```powershell
cd E:\SIS\backend
mvn -q test
mvn spring-boot:run
```

Frontend:

```powershell
cd E:\SIS\frontend
npm install
npm run dev
```

Open the frontend at `http://localhost:5173`. The Vite dev server proxies `/api` to `http://localhost:8080`.

Default local login seeded by the backend:

- Username: `analyst`
- Password: `Password123!`

## Docker Compose

Create a private `.env` from the example before running compose:

```powershell
cd E:\SIS
Copy-Item .env.example .env
# edit .env and replace the JWT secret/passwords for any non-demo use
docker compose up --build
```

Services:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api/v1`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

The compose defaults are for local development only. Replace database passwords and `IIDS_SECURITY_JWT_SECRET` before using the stack outside a private machine.

## M1 Workflow Coverage

Implemented backend capabilities:

- JWT login and RBAC foundation
- Project and scenario management
- Scenario parameters, investment items and financing plans
- Financial calculation tasks with persisted metrics and cash-flow rows
- Excel import jobs
- Report generation and Excel download
- Fixed approval chain
- Scenario edit lock
- Audit events

Implemented frontend foundation:

- Login flow with token storage
- Project/scenario workbench
- Parameter, investment and financing input forms
- Calculation task execution, metric table, cash-flow table and ECharts metric chart
- Report generation/download entry
- Approval, edit lock and audit query entry points

## Verification

Backend:

```powershell
cd E:\SIS\backend
mvn -q test
mvn -q -DskipTests package
```

Frontend:

```powershell
cd E:\SIS\frontend
npm run build
```

Compose configuration:

```powershell
cd E:\SIS
docker compose config
```

Current verification note: backend tests/package and frontend build passed on 2026-07-27. `docker compose config` could not be completed on the current machine because the `docker` command is not available in PowerShell.
