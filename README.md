# NGS Analytics Platform

Local-first platform for FASTQ/VCF quality and variant metrics, with a Spring Boot API and Angular dashboards.

Designed to run on a **16 GB RAM Windows laptop** (modular monolith with streaming Java parsers).

## Stack

- **Backend:** Java 21, Spring Boot 3, JWT, JPA, PostgreSQL
- **Frontend:** Angular 19 + Apache ECharts
- **Infra:** Docker Compose (Postgres; optional RabbitMQ/MinIO/Prometheus/Grafana)
- **Jobs:** Java streaming parsers

## Quick start (Phase 1)

### Prerequisites

- JDK 21, Maven 3.9+, Node 22+, Docker Desktop (limit memory to **6 GB**)

### 1. Database

**Option A — PostgreSQL (recommended):** start Docker Desktop, then:

```bash
cd docker
docker compose up -d postgres
```

**Option B — H2 file DB (no Docker):** skip Compose and use profile `dev-h2` in step 2.

### 2. Start API

```bash
cd backend
mvn spring-boot:run
```

Without Docker:

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=dev-h2"
```

API: `http://localhost:8080`  
Swagger: `http://localhost:8080/swagger-ui.html`

### 3. Start UI

```bash
cd frontend
npm start
```

UI: `http://localhost:4200`

### 4. Try fixtures

1. Register a user in the UI
2. Create a project + sample
3. Upload [`datasets/sample.fastq`](datasets/sample.fastq) and [`datasets/sample.vcf`](datasets/sample.vcf)
4. Wait a few seconds for analysis status `DONE` and open charts

## Phase 2 (async + optional MinIO)

```bash
cd docker
docker compose --profile async up -d
cd ../backend
mvn spring-boot:run "-Dspring-boot.run.profiles=async"
```

## Phase 4 observability

```bash
cd docker
docker compose --profile obs up -d
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001` (admin/admin)
- API metrics: `http://localhost:8080/actuator/prometheus`

## Tests

```bash
cd backend && mvn test
cd ../frontend && npm test -- --watch=false --browsers=ChromeHeadless
```

## Repository layout

See [`docs/architecture.md`](docs/architecture.md).

## Hardware notes

| Resource | Guidance |
|----------|----------|
| RAM | Keep Docker Desktop ≤ 6 GB; API heap ~1 GB |
| Disk | Use tiny fixtures in `datasets/`; prune `data/uploads` |
| GPU | Not used |

## Default credentials (local Compose)

| Service | User | Password |
|---------|------|----------|
| PostgreSQL | `ngs` | `ngs` |
| RabbitMQ | `ngs` | `ngs` |
| MinIO | `ngsminio` | `ngsminio123` |
