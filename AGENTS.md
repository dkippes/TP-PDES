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

## Design and scope

Apply the [design and quality guidelines](Docs/design-guidelines.md) when implementing or reviewing code.

- Implement the agreed ticket scope. The roadmap and enunciado do not authorize adding adjacent features; clarify dependencies before expanding scope.
- Apply SOLID with cohesive responsibilities, composition and constructor injection. Introduce interfaces and patterns for real boundaries or variations, not speculative extension points.
- Keep Kotlin domain invariants in services and preserve `Controller -> Service -> Repository`. Keep transport DTOs separate from JPA entities, with explicit create/update contracts when their requirements differ.
- In React, compose function components and hooks. Keep HTTP/session mechanics separate from user/agency operations; give components only the API capabilities they consume.
- Extract reusable presentation components when they reduce responsibilities in a panel. Avoid a generic CRUD framework for two resources with distinct rules.
- Verify observable behavior, permissions, failure handling and idempotent fixture loading. Keep test setup isolated and share existing test utilities rather than adding another framework.

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
- `GET /api/paquetes`, `GET /api/paquetes/buscar` and `GET /api/agencias` (list only) are explicitly public in `SecurityConfig`. `/buscar` accepts optional filters combined with AND: `q` (case-insensitive contains over destination, origin, hotel name/country and agency name), `destino` and `origen` (case-insensitive exact match), `agenciaId`, `precioMin`/`precioMax` (inclusive), `fechaDesde`/`fechaHasta` (ISO dates over `fechaInicial`/`fechaFinal`), `orden` (`FECHA_ASC` default, `FECHA_DESC`, `PRECIO_ASC`, `PRECIO_DESC`, `DESTINO_ASC`; id breaks ties), and `pagina`/`tamanio` (defaults 0 and 12, size 1 to 50). It returns `{contenido, pagina, tamanio, totalElementos, totalPaginas}`, with `contenido` in the same shape as the catalog, and HTTP 400 for invalid parameters or inverted ranges. Other catalog permissions are declared per HTTP method/path in `CatalogEndpointRules`: agency detail, hotel list/detail and package detail require any of the three authenticated roles; only ADMINISTRADOR may create/delete agencies and write hotels; ADMINISTRADOR/AGENTE may update agencies and write packages. Unlisted endpoints remain denied. CRUD tests send production-signed JWTs through the real filter chain.

## Frontend conventions

- Write React function components in TypeScript.
- Use hooks and local state unless a feature demonstrates the need for shared state. No Redux or Context architecture is established yet.
- Define component props with TypeScript types.
- Keep reusable components in `front/src/components/` and page-level views in `front/src/pages/`.
- Put shared TypeScript models in `front/src/types.ts` or a focused type module when that file becomes too broad.
- Use `import.meta.env` for Vite environment variables. The main backend base URL is `VITE_API_URL`.
- Prefer the established Tailwind approach for UI styling. Avoid introducing a second styling system without an explicit reason.
- Preserve the ESLint rules for TypeScript, React Hooks, and React Refresh.
- Authentication integration tests use Node's built-in test runner through `npm test`; CI runs them alongside lint/build.
- The frontend uses the existing `/api/auth/register`, `login`, `refresh`, and `logout` endpoints. Registration creates an account and redirects to login.
- Keep access tokens in memory. Restore and renew sessions through the HttpOnly refresh cookie with `credentials: 'include'`; do not trust a user object in localStorage as authentication.
- Inicio renders `HomePage` with a `PaquetesSearchBar` (origin/destination with datalist suggestions derived from the catalog by `hooks/usePaquetesCatalogo.ts`, date range, popular-destination chips) above all packages for visitors and authenticated users through the public `api/paquetes.ts` contract. Origin/destination fields use the `SugerenciasInput` combobox (case- and accent-insensitive). A known value is sent as exact `destino`/`origen`; free-text destinations are sent as `q`, and unknown origins are rejected inline because `q` would also match packages travelling to that place. Searching opens the `resultados` view (`ResultadosPage`, state held in `App`, no router), which consumes `/api/paquetes/buscar` and `/api/agencias` with side filters (keyword, agency, price range), active-filter chips, ordering and pagination; changing filters resets to page 0. Cards show destination/country, dates, agency, price, a description derived from origin/destination/hotel, and a local placeholder image. ADMINISTRADOR opens `AdminHomePage` with user/agency CRUD through Administración in the navbar; Inicio returns to the catalog. `hooks/useRemoteData.ts` shares loading/error/retry state. `api/http.ts` centralizes JSON transport and errors; `api/authenticated.ts` sends the in-memory Bearer token, shares the rotating refresh on HTTP 401 and retries once. `api/admin.ts` maps resource operations through an injected request and exposes separate `UsuariosApi` / `AgenciasApi` contracts. Transport DTOs live in `types/admin.ts`; user creation requires a password and updates may omit it. User administration returns `rol`, whereas authentication returns `user.role`.
- Admin forms leave an unchanged password out of PUT requests, require an agency for AGENTE, and display backend validation/conflict errors. Editing the current account updates the navbar; resetting its password returns to login.
- `App` coordinates navigation and notices; `hooks/useSession.ts` owns session restoration, renewal, logout and current-account updates. `AppLayout` handles the shared shell. Pages compose focused components: `GuestInvitation` and `PaquetesCatalog` for Inicio, `AuthCard` and separate forms for authentication, and header/sections/panels for administration. Domain hooks under `hooks/auth/` and `hooks/admin/` own form/panel state and operations. `useAsyncAction` shares pending/error handling; `FormField` shares labeled inputs while callers retain their validation. Preserve the existing HTTP boundaries and resource-specific contracts.
- Only authenticated COMPRADOR accounts see heart buttons and Mis favoritos. `CompradorPage` shares `useFavoritos` state between the catalog and favorites view; it is keyed by user ID and unmounts on logout. `api/favoritos.ts` uses the existing authenticated transport. Loading/failure states disable hearts until the list is known. Successful mutations update local data after server confirmation, without reloading or hiding cards; their pending status is announced to screen readers without shifting the layout. `useRemoteData.updateData` prevents an older read from overwriting a confirmed mutation. Failed mutations preserve saved state and display the error.

## Favorites

- `GET /api/favoritos` returns only the current buyer's favorites, newest first, as `{ paquete: { id, origen, destino, hotel: { nombre } }, agregadoEn }` DTOs. `PUT /api/favoritos/{paqueteId}` and `DELETE /api/favoritos/{paqueteId}` are idempotent and return 204. Saving a missing package returns 404; removing an absent favorite or deleted package returns 204. IDs must be positive.
- All three operations require COMPRADOR. The service obtains ownership from the signed JWT and verifies that the persisted account still has a buyer profile. It accepts no buyer ID from the client. Writes lock the user's row to serialize concurrent requests and the schema enforces one favorite per buyer/package.
- Migration `V5__create_favorito.sql` uses `ON DELETE CASCADE` for buyer and package references. Deleting packages (including through agency deletion), deleting buyers or replacing their profile clears the corresponding favorites. Existing restrictions for purchases/reviews remain in force. Matching Hibernate `@OnDelete` mappings preserve this behavior in schema-generated H2 tests.
- Favorites store interest, use current package data and do not reserve flights or fix a price. Administrator consultation, purchases and reviews belong to separate tickets. Keep the six existing Gherkin scenarios; favorites contract tests use the existing Node runner and backend behavior/security tests use MockMvc and production-signed JWTs.

## Flight sales

- `POST /vuelos/venta` on the flying service sells one seat on every flight in `{ operacionId, vueloIds }`, all or nothing, and returns `{ operacionId, vueloIds, creadaEn }`. It returns 201 for a new sale, 400 for an empty list or duplicated/non-positive IDs, 404 when a flight is missing and 409 when a flight has no availability; nothing is sold on failure.
- Availability is checked and decremented with one conditional UPDATE per flight (`VueloRepository.descontarAsiento`), in ascending ID order. Do not replace it with read-then-save: concurrent sales would oversell.
- `operacionId` (UUID) is an idempotency key persisted by migration `V2__create_venta.sql`. Repeating it with the same flights returns the original sale with 200 and sells nothing; with different flights it returns 409, as does a concurrent request still holding the same key. Sold flights are stored by ID without a foreign key, so flight deletion is unchanged.

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

With the `dev` Spring profile (Compose's default), `bootstrap/DevDataSeeder` creates missing demo accounts: `admin@gmail.com` (ADMINISTRADOR), `usuario@gmail.com` (COMPRADOR), `agente@gmail.com` and `agente.andino@gmail.com` (AGENTE), all with password `12345678`. It also creates Viajes del Sur, Rumbo Andino and Horizonte Turismo; the agents belong to the first two agencies. Horizonte starts without agents so its deletion can be exercised. Existing records are preserved by email; missing demo records are recreated on startup. Data is persisted in PostgreSQL through repositories after Flyway; the seeder is disabled outside `dev`.

`bootstrap/DevPaquetesSeeder` creates hotels and packages for Bariloche, Córdoba and Madrid, using available future round-trip flights from flying-service through `VueloClient` and the existing `PaqueteService` validations. It preserves existing packages by agency/hotel/route across restarts. An unavailable flight service or a missing route skips package fixtures until a later startup; it does not prevent creating demo users/agencies. Existing flight fixtures must still include future flights. No cross-service database access is used.

## Build and verification commands

Run the smallest relevant checks first, then the complete checks for each touched application.

### Frontend

```bash
cd front
npm ci
npm run lint
npm test
npm run build
npm run test:bdd
```

Gherkin tests require preinstalled Chrome and port 15174 to be free. See `Docs/gherkin.md` for scope and reports.

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

CI covers frontend lint/test/build, application-backend test/build, and flying-service test/build checks.

CD (`.github/workflows/cd.yml`) runs on pushed tags matching `v*.*.*` and also when a PR from a `release/X.Y.Z` branch is merged into `main`: the `tag` job then creates tag `vX.Y.Z` on the merge commit (fails if it already exists or the branch name is not semver) and publishes in the same run, since tags created with `GITHUB_TOKEN` do not trigger other workflows. It fails unless the tagged commit is on `main`, and publishes three Docker Hub images (`aterrizar-backend`, `aterrizar-flying-service`, `aterrizar-front`) tagged `X.Y.Z`, `X.Y` and `latest`. It requires GitHub Actions secrets `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN`. Create the Docker Hub repositories manually as private beforehand (a first push would create them public; the free plan allows only one private repo).

`docker-compose.release.yml` is the stack for external users: it uses only the published images, has defaults for every variable (no `.env`) and no bind mounts. After the images, the CD publishes it with `docker compose publish` as OCI artifact `<usuario>/aterrizar-compose:X.Y.Z` (and `latest`), replacing the `your-dockerhub-user` placeholder and pinning the default `VERSION` to the tag. Users run `docker compose -f oci://<usuario>/aterrizar-compose:X.Y.Z up -d` and open `http://localhost:5173`. Keep `ports` in long syntax there: `docker compose publish` fails to parse short-syntax ports that contain variables. Requires Docker Compose 2.34+ on the consumer side (not verified against a real Docker Hub publish yet).

Each service `Dockerfile` is multi-stage: `target: dev` (used by `docker-compose.yml`, code mounted as a volume, `bootRun`/Vite) and a final `runtime` stage that is self-contained (Kotlin services: `bootJar` on `eclipse-temurin:21-jre`; front: Vite build served by nginx on port 80). The default build, used by CI and CD, is `runtime`. The front bakes `VITE_API_URL` at build time through the `VITE_API_URL` build arg (default `http://localhost:8080`), so a published front image is tied to that backend URL.

Code quality uses SonarQube Cloud project `dkippes_TP-PDES` in organization `dkippes` for the three applications. Root `sonar-project.properties` defines the scope; `.github/workflows/sonar.yml` runs on pushes/PR targeting `develop` without path filters and publishes the `Sonar Quality Gate` check. Setup and limitations are documented in `Docs/sonar.md`. Configure GitHub Actions secret `SONAR_TOKEN`; disable Sonar automatic analysis and align its main branch with `develop`.

Both Kotlin services use JaCoCo 0.8.15: `gradlew.bat check sonarDependencies` generates XML/HTML coverage, enforces at least 80% total LINE COVEREDRATIO separately for each service, and exports dependency JARs for the root scanner. `check`/`build` depend on `jacocoTestCoverageVerification`; no Kotlin classes are excluded. Frontend coverage is intentionally deferred: `sonar.coverage.exclusions=front/src/**` excludes only coverage, not static analysis. Keep frontend lint/test/build; do not claim frontend coverage until LCOV is configured.

## Frontend behavior tests (Gherkin)

Only registration and login have Spanish Gherkin scenarios under `front/bdd/`, executed with `playwright-bdd` and preinstalled Chrome. Keep the six existing scenarios; do not add Gherkin scenarios for other functionality without an explicit request. `front/playwright.bdd.config.ts` selects those two feature files and starts only Vite on port 15174. Step definitions intercept API requests with Playwright routes; these tests verify frontend behavior with simulated responses, not backend authentication or persistence. Node contract/session tests remain separate under `front/tests/`.

Run `npm run test:bdd` from `front/`. It typechecks steps, generates Playwright tests from features, and runs them. Generated `.features-gen/` is ignored. Reports are saved in `front/playwright-bdd-report/` and failures in `front/test-results-bdd/`. See `Docs/gherkin.md` for scope and commands. No Java, Docker or backend startup is needed.

Use `npm run test:bdd:debug` to open Chrome and the Playwright Inspector and step through browser actions. This script includes `--debug` directly rather than relying on npm argument forwarding.

## Environment and secrets

Use `.env.example`, `docker-compose.yml`, and each service's `application.properties` together as the environment-variable sources of truth. Keep real credentials out of version control. Relevant variable groups include:

- application PostgreSQL: `POSTGRES_*` and `SPRING_DATASOURCE_*`
- flying PostgreSQL: `FLYING_POSTGRES_*`
- service addresses and ports: `BACKEND_PORT`, `FLYING_SERVICE_PORT`, `FLYING_SERVICE_URL`, `FRONT_PORT`
- frontend API addresses: `VITE_API_URL`; Compose also defines `VITE_FLYING_SERVICE_URL`, but the frontend does not currently consume it
- browser access: `CORS_ALLOWED_ORIGIN`, derived by Compose from `FRONT_PORT`

JWT authentication, role authorization, and rotating refresh sessions are implemented under `backend/.../auth/` and `security/`. Their settings include `JWT_SECRET`, `JWT_EXPIRATION_MS`, `REFRESH_TOKEN_EXPIRATION_MS`, and `REFRESH_COOKIE_*`. Treat Datadog, Grafana, and AWS as planned or unverified until source code and deployment configuration establish them.

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
