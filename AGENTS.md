# AterrizAR agent guide

Use this file as the starting point for repository work. Confirm behavior in source and configuration before changing it; the root `README.md` includes planned technologies that are not all implemented yet.

## Project map

| Path | Responsibility | Main stack |
| --- | --- | --- |
| `front/` | Browser UI | React 19, TypeScript, Vite, Tailwind CSS, ESLint |
| `backend/` | Public application API and orchestration | Kotlin, Java 21, Spring Boot, Spring MVC, JPA, Flyway, PostgreSQL |
| `flying.service/` | Internal flight data API | Kotlin, Java 21, Spring Boot, Spring MVC, JPA, Flyway, PostgreSQL |
| `Docs/` | Domain and data-model diagrams | SVG, PNG, and diagram source files |
| `.github/workflows/` | CI pipelines | GitHub Actions |
| `docker-compose.yml` | Local development topology | Docker Compose, PostgreSQL 16 |

Both Kotlin services currently declare Kotlin 2.3.21 and Spring Boot 4.1.0 in their Gradle builds.

## Runtime architecture

```text
Browser / React frontend (:5173)
        |
        | HTTP, VITE_API_URL
        v
Application backend (:8080)
        |
        | HTTP, FLYING_SERVICE_URL
        v
Flying service (:8081)

Application backend -> application PostgreSQL
Flying service      -> flying PostgreSQL
```

The intended backend layering is:

```text
Controller -> Service -> Repository -> Database
```

Keep transport concerns in controllers, business rules in services, and persistence access in repositories. The flight CRUD implementation under `flying.service/.../vuelo/` is the current reference. The application backend's `PingController` is an early integration path and currently accesses `RestTemplate` and `PingLogRepository` directly; do not copy that shortcut into new domain features.

### Current integration path

1. `front/src/components/Ping.tsx` calls `POST /api/ping` on the application backend.
2. `backend/.../PingController.kt` calls `GET /ping` on the flying service.
3. The application backend records the result as a `PingLog`.
4. A failed flying-service response is surfaced by the application backend as HTTP `503`.

Public application-backend endpoints currently use the `/api` prefix. Internal flying-service endpoints currently use direct paths such as `/ping` and `/vuelos`.

## Backend conventions

These rules apply to both Kotlin services unless a local package already establishes a stronger convention.

- Use Kotlin naming conventions: `PascalCase` types and `camelCase` functions/properties.
- Keep package names lowercase. Preserve each service's existing package root:
  - application backend: `com.aterrizAR.backend`
  - flying service: `com.backend.flying.service`
- Implement new domain flows as `Controller -> Service -> Repository`.
- Use Spring Data `JpaRepository` for persistence repositories.
- Keep request payloads separate from JPA entities. The flight service uses `VueloRequest` for validated input and `Vuelo` as the entity.
- Name new transport DTO classes with the `DTO` suffix. Input DTOs expose `toModel()` only when they map to a domain model; domain models expose `toDTO()` for response mapping. Do not add artificial conversions for command payloads such as login credentials.
- Apply Jakarta Validation annotations at request boundaries and use `@Valid` in controllers.
- Keep business invariants in services. Existing example: flight availability must not exceed capacity.
- Match HTTP behavior explicitly. The current flight service uses `ResponseStatusException` for service-level not-found and validation failures; no shared global exception hierarchy is established yet.
- Keep OpenAPI annotations aligned with endpoint behavior where that convention already exists.
- Treat Flyway migrations as the schema history. Add migrations under `src/main/resources/db/migration/` using `V<version>__<description>.sql`; do not rewrite applied migrations.

### Backend configuration

- Application backend default port: `8080`.
- Flying service default port in Compose: `8081`.
- The application backend reads the internal service base URL from `FLYING_SERVICE_URL`.
- Database connection settings use Spring's `SPRING_DATASOURCE_*` variables.
- CORS is configured through `CORS_ALLOWED_ORIGIN`.
- Normal application properties default Hibernate schema handling to `validate`; local Compose currently overrides the application backend to `update`. Do not assume development behavior is suitable for production.

## Frontend conventions

- Write React function components in TypeScript.
- Use hooks and local state unless a feature demonstrates the need for shared state. No Redux or Context architecture is established yet.
- Define component props with TypeScript types.
- Keep reusable components in `front/src/components/` and page-level views in `front/src/pages/`.
- Put shared TypeScript models in `front/src/types.ts` or a focused type module when that file becomes too broad.
- Use `import.meta.env` for Vite environment variables. The main backend base URL is `VITE_API_URL`.
- Prefer the established Tailwind approach for UI styling. Avoid introducing a second styling system without an explicit reason.
- Preserve the ESLint rules for TypeScript, React Hooks, and React Refresh.
- There is currently no frontend test runner configured. If adding one, document the command and CI integration in the same change.

## Local development

The repository-level development path is Docker Compose:

```bash
cp .env.example .env
docker compose up --build --watch
```

Expected local services:

| Service | URL |
| --- | --- |
| Frontend | `http://localhost:5173` |
| Application health | `http://localhost:8080/api/health` |
| Application integration ping | `POST http://localhost:8080/api/ping` |
| Flying service ping | `http://localhost:8081/ping` |
| Application PostgreSQL | `localhost:5432` by default |

Compose maintains separate PostgreSQL databases and volumes for the application backend and the flying service. Do not couple their schemas or access one service's database directly from the other service.

## Build and verification commands

Run the smallest relevant checks first, then the complete checks for each touched application.

### Frontend

```bash
cd front
npm ci
npm run lint
npm run build
```

Use `npm run dev` for local Vite development and `npm run preview` to inspect a production build.

### Application backend

```bash
cd backend
./gradlew test
./gradlew build
```

On Windows without a POSIX shell, use `gradlew.bat` instead of `./gradlew`.

### Flying service

```bash
cd flying.service
./gradlew test
./gradlew build
```

On Windows without a POSIX shell, use `gradlew.bat` instead of `./gradlew`.

CI currently covers frontend lint/build and application-backend test/build checks. `flying.service` has no dedicated CI workflow.

## Environment and secrets

Use `.env.example`, `docker-compose.yml`, and each service's `application.properties` together as the environment-variable sources of truth. Keep real credentials out of version control. Relevant variable groups include:

- application PostgreSQL: `POSTGRES_*` and `SPRING_DATASOURCE_*`
- flying PostgreSQL: `FLYING_POSTGRES_*`
- service addresses and ports: `BACKEND_PORT`, `FLYING_SERVICE_PORT`, `FLYING_SERVICE_URL`, `FRONT_PORT`
- frontend API addresses: `VITE_API_URL`; Compose also defines `VITE_FLYING_SERVICE_URL`, but the frontend does not currently consume it
- browser access: `CORS_ALLOWED_ORIGIN`, derived by Compose from `FRONT_PORT`

`JWT_SECRET` and `JWT_EXPIRATION_MS` are present in project documentation/Compose configuration, but authentication code was not identified during the initial repository survey. Treat JWT, Datadog, Grafana, and AWS as planned or unverified until source code and deployment configuration establish them.

## Change checklist

Before finishing a change:

1. Preserve service boundaries: frontend calls the application backend; the application backend calls the flying service.
2. Follow `Controller -> Service -> Repository` for new backend domain behavior.
3. Add a Flyway migration for schema changes.
4. Update request validation and OpenAPI documentation together when endpoint contracts change.
5. Run the checks for every affected application.
6. Update this guide when introducing a new tool, architectural pattern, environment variable, or required command.

## Sources of truth

Prefer evidence in this order:

1. Executable source and tests.
2. Build files and runtime configuration.
3. Database migrations.
4. CI workflows.
5. Root documentation and diagrams.

When these disagree, do not silently choose one. Call out the discrepancy and either align them in the same change or document it as follow-up work.
