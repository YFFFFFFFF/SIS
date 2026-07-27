# API Overview

This document summarizes the M1 HTTP API implemented by the backend. The backend base path is `/api/v1`.

## Common Rules

- Authentication: `POST /api/v1/auth/login` is public. Other `/api/v1/**` endpoints require `Authorization: Bearer <token>`.
- Admin access: `/api/v1/admin/**` requires the `ADMIN` role.
- Response envelope:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

- Error responses use the same envelope shape with a non-success `code`.
- JSON request bodies use camelCase field names.

## Auth

| Method | Path | Body | Result |
| --- | --- | --- | --- |
| POST | `/auth/login` | `username`, `password` | JWT token, token type, username, display name, roles |
| GET | `/admin/ping` | none | `"pong"` for ADMIN users |

Example login:

```json
{
  "username": "analyst",
  "password": "Password123!"
}
```

Seeded local accounts:

| Username | Password | Role |
| --- | --- | --- |
| `analyst` | `Password123!` | `ANALYST` |
| `admin` | `Password123!` | `ADMIN` |

## Projects

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/projects` | List projects |
| GET | `/projects/{id}` | Get one project |
| POST | `/projects` | Create project |
| PUT | `/projects/{id}` | Update project |

Create body:

```json
{
  "code": "P-001",
  "name": "New Energy Plant",
  "projectType": "INDUSTRIAL",
  "department": "Investment",
  "ownerId": 1,
  "tags": "m1,benchmark",
  "description": "Demo project"
}
```

Update body:

```json
{
  "name": "New Energy Plant",
  "projectType": "INDUSTRIAL",
  "status": "ACTIVE",
  "department": "Investment",
  "ownerId": 1,
  "tags": "m1,benchmark",
  "description": "Demo project"
}
```

Project status values: `DRAFT`, `ACTIVE`, `ARCHIVED`.

## Scenarios And Parameters

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/projects/{projectId}/scenarios` | List scenarios under a project |
| POST | `/projects/{projectId}/scenarios` | Create scenario |
| GET | `/scenarios/{id}` | Get one scenario |
| PUT | `/scenarios/{id}` | Update scenario |
| GET | `/scenarios/{id}/parameters` | Get parameter set |
| PUT | `/scenarios/{id}/parameters` | Create or update parameter set |

Scenario create body:

```json
{
  "name": "Base Case",
  "horizonYears": 5,
  "constructionYears": 1,
  "remarks": "M1 baseline"
}
```

Scenario update body:

```json
{
  "name": "Base Case",
  "status": "DRAFT",
  "horizonYears": 5,
  "constructionYears": 1,
  "remarks": "M1 baseline"
}
```

Scenario status values: `DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED`.

Parameter body:

```json
{
  "wacc": 0.1,
  "waccSource": "manual benchmark",
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

## Calculation Inputs And Tasks

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/scenarios/{scenarioId}/investment-items` | Add investment item |
| POST | `/scenarios/{scenarioId}/financing-plans` | Add financing plan |
| POST | `/scenarios/{scenarioId}/calculation-tasks` | Create async calculation task |
| GET | `/calculation-tasks/{taskId}` | Query task status |
| GET | `/calculation-tasks/{taskId}/results` | Query metrics and cash-flow rows |

Investment item body:

```json
{
  "category": "CONSTRUCTION",
  "name": "Construction Investment",
  "amount": 200000,
  "yearNo": 0
}
```

Financing plan body:

```json
{
  "sourceType": "EQUITY",
  "ratio": 1,
  "amount": 220000,
  "interestRate": 0,
  "termYears": 0
}
```

Calculation task body:

```json
{
  "taskType": "FINANCIAL",
  "requestKey": "manual-001"
}
```

Task status values: `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`.

Calculation result contains:

- `task`: task status and progress.
- `metrics`: metric code to numeric value map.
- `cashFlowRows`: period cash-flow rows with inflow, outflow, net cash flow, discounted cash flow and cumulative cash flow.

## Import And Reports

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/scenarios/{scenarioId}/import/excel` | Upload Excel file as multipart field `file` |
| GET | `/import-jobs/{jobId}` | Query import job |
| POST | `/calculation-tasks/{taskId}/reports` | Generate Excel report |
| GET | `/reports/{reportId}/download` | Download generated Excel report |

The download endpoint returns binary Excel content with `Content-Disposition: attachment`.

## Governance

| Method | Path | Body | Purpose |
| --- | --- | --- | --- |
| POST | `/scenarios/{scenarioId}/approval/submit` | optional `comment` | Submit scenario to fixed approval chain |
| POST | `/approval-instances/{instanceId}/review/approve` | optional `comment` | Review approval |
| POST | `/approval-instances/{instanceId}/approve` | optional `comment` | Final approval |
| POST | `/approval-instances/{instanceId}/reject` | optional `comment` | Reject approval instance |
| POST | `/scenarios/{scenarioId}/lock` | `holderId`, `holderName`, `ttlMinutes` | Acquire scenario edit lock |
| DELETE | `/scenarios/{scenarioId}/lock` | `holderId` | Release scenario edit lock |
| GET | `/audit-events?targetType={type}&targetId={id}` | none | Query audit events |

Approval comment body:

```json
{
  "comment": "Approved"
}
```

Acquire lock body:

```json
{
  "holderId": 1,
  "holderName": "Analyst",
  "ttlMinutes": 30
}
```

Release lock body:

```json
{
  "holderId": 1
}
```
