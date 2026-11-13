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
- `GET /api/paquetes`, `GET /api/paquetes/buscar`, `GET /api/paquetes/{id}` and `GET /api/agencias` (list only) are explicitly public in `SecurityConfig`. `/buscar` accepts optional filters combined with AND: `q` (case-insensitive contains over destination, origin, hotel name/country and agency name), `destino` and `origen` (case-insensitive exact match), `agenciaId`, `precioMin`/`precioMax` (inclusive), `fechaDesde`/`fechaHasta` (ISO dates over `fechaInicial`/`fechaFinal`), `orden` (`FECHA_ASC` default, `FECHA_DESC`, `PRECIO_ASC`, `PRECIO_DESC`, `DESTINO_ASC`; id breaks ties), and `pagina`/`tamanio` (defaults 0 and 12, size 1 to 50). It returns `{contenido, pagina, tamanio, totalElementos, totalPaginas}`, with `contenido` in the same shape as the catalog, and HTTP 400 for invalid parameters or inverted ranges. Other catalog permissions are declared per HTTP method/path in `CatalogEndpointRules`: agency detail and hotel list/detail require any of the three authenticated roles; only ADMINISTRADOR may create/delete agencies and write hotels; ADMINISTRADOR/AGENTE may update agencies and write packages. Unlisted endpoints remain denied. CRUD tests send production-signed JWTs through the real filter chain.

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
- Admin forms leave an unchanged password out of PUT requests, require an agency for AGENTE, and display backend validation/conflict errors. A user's role is chosen at creation and is read-only when editing any account. PUT keeps the existing role in the payload; the backend rejects a different role with 409 before modifying the account. Agents may still change agencies while retaining their profile. Editing the current account updates the navbar; resetting its password returns to login.
- `App` coordinates navigation and notices; `hooks/useSession.ts` owns session restoration, renewal, logout and current-account updates. `AppLayout` handles the shared shell. Pages compose focused components: `GuestInvitation` and `PaquetesCatalog` for Inicio, `AuthCard` and separate forms for authentication, and header/sections/panels for administration. Domain hooks under `hooks/auth/` and `hooks/admin/` own form/panel state and operations. `useAsyncAction` shares pending/error handling; `FormField` shares labeled inputs while callers retain their validation. Preserve the existing HTTP boundaries and resource-specific contracts.
- Package cards in Inicio, resultados and favoritos link to the public `/paquetes/{id}` page (`PaquetePage` / `PaqueteDetalle`). It fetches the current package through `api/paquetes.obtener` and shows origin, destination, dates, price, hotel/photo and agency. Missing or broken hotel photos use the local placeholder; missing/invalid IDs show not-found, and load errors allow retry. `hooks/useNavigation.ts` uses browser History API for the detail URL and stores the other views and search filters in history state, preserving ordering/pagination on return and browser back/forward. Direct links and reloads work with the existing Vite/nginx SPA fallback; a direct link returns to Inicio. Native card links support opening another tab, with independent favorite and purchase buttons. COMPRADOR detail reuses `CompradorPage` favorites, purchase history and its single purchase dialog; other roles and visitors only see the detail.
- Only authenticated COMPRADOR accounts see heart buttons and Mis favoritos. `CompradorPage` shares `useFavoritos` state between the catalog and favorites view; it is keyed by user ID and unmounts on logout. `api/favoritos.ts` uses the existing authenticated transport. Loading/failure states disable hearts until the list is known. Successful mutations update local data after server confirmation, without reloading or hiding cards; their pending status is announced to screen readers without shifting the layout. `useRemoteData.updateData` prevents an older read from overwriting a confirmed mutation. Failed mutations preserve saved state and display the error.

## Agency workspace

- `POST /api/paquetes`, `PUT /api/paquetes/{id}` and `DELETE /api/paquetes/{id}` go through `PaqueteAgenciaService`: ADMINISTRADOR manages any agency's packages, an AGENTE only those of its own agency (403 otherwise, including moving a package to another agency). A package that has purchases can be neither edited nor deleted (409): buyers see the current package data in their history, so it must not change under them. `Mis paquetes` derives the purchase count per package from `GET /api/ventas`, makes Editar/Eliminar inert for those rows (`aria-disabled`, still focusable) and the "Acciones" column header carries an info icon with a hover/focus tooltip that explains the rule (`components/Tooltip.tsx`, floating through `hooks/useFloatingPanel.ts`); the backend remains the authority.
- `GET /api/vuelos` (ADMINISTRADOR/AGENTE, rule in `VueloEndpointRules`) proxies flying-service's flight list through `VueloService`, soonest first, as `{ id, origen, destino, fecha, hora, aerolinea, disponibilidad }`; 503 when flying-service is unreachable.
- `GET /api/ventas` (AGENTE only, rule in `VentaEndpointRules`) returns the sales of the agent's agency, newest first, as `{ id, operacionId, precio, fecha, paqueteId, origen, destino, hotel, cliente: { id, nombre, apellido, correo } }`.
- `POST /api/auth/register-agencia` (public) takes `{ usuario: <register body>, agencia: { nombre, email } }` and creates the agency and its first AGENTE in one transaction. It returns 201 with the public user, 409 for a duplicated user or agency email, and 400 with `usuario.*` / `agencia.*` field errors.
- Frontend: `/agencia` (`AgenciaPage`, AGENTE only, "Mi agencia" in the navbar) reads the agency from `GET /api/usuarios/me` and has "Mis paquetes" (table, pagination, create/edit/delete via `api/agencia.ts`; the form asks first for the trip dates with the custom month calendar `components/forms/DateRangePicker.tsx` (pick "from" then "to"; days with available flights are marked; helpers in `components/forms/calendario.ts`), then offers the outbound flight from `GET /api/vuelos` inside them (which fixes origin/destination) and a return flight on the reverse route, inside the dates and on or after the outbound one; only flights with seats are offered and changing the dates drops flights that no longer fit, see `components/agencia/vuelos.ts`; its dropdowns use the custom accessible `components/forms/Select.tsx` instead of the native select; options carry a label, a secondary line and a status badge, and the list and calendar float with fixed position through `hooks/useFloatingPanel.ts` so they never grow or scroll the dialog) and "Ventas y clientes" (sales plus clients summarized in the frontend by `resumirClientes`). `/registro-agencia` (`RegisterAgenciaPage`, linked from `/registro`) registers an agency and redirects to login like the buyer registration. `npm run test:agencia` runs the Playwright smoke tests with intercepted API (Vite on port 15177).

## Favorites

- `GET /api/favoritos` returns only the current buyer's favorites, newest first, as `{ paquete: { id, origen, destino, hotel: { nombre } }, agregadoEn }` DTOs. `PUT /api/favoritos/{paqueteId}` and `DELETE /api/favoritos/{paqueteId}` are idempotent and return 204. Saving a missing package returns 404; removing an absent favorite or deleted package returns 204. IDs must be positive.
- All three operations require COMPRADOR. The service obtains ownership from the signed JWT and verifies that the persisted account still has a buyer profile. It accepts no buyer ID from the client. Writes lock the user's row to serialize concurrent requests and the schema enforces one favorite per buyer/package.
- Migration `V5__create_favorito.sql` uses `ON DELETE CASCADE` for buyer and package references. Deleting packages (including through agency deletion) or deleting buyers clears the corresponding favorites. Role-change attempts are rejected and preserve the buyer's profile and favorites. Existing restrictions for purchases/reviews remain in force. Matching Hibernate `@OnDelete` mappings preserve deletion behavior in schema-generated H2 tests.
- Favorites store interest, use current package data and do not reserve flights or fix a price. Administrator consultation, purchases and reviews belong to separate tickets. Keep the six existing Gherkin scenarios; favorites contract tests use the existing Node runner and backend behavior/security tests use MockMvc and production-signed JWTs.

## Flight sales

- `POST /vuelos/venta` on the flying service sells one seat on every flight in `{ operacionId, vueloIds }`, all or nothing, and returns `{ operacionId, vueloIds, creadaEn }`. It returns 201 for a new sale, 400 for an empty list or duplicated/non-positive IDs, 404 when a flight is missing and 409 when a flight has no availability; nothing is sold on failure.
- Availability is checked and decremented with one conditional UPDATE per flight (`VueloRepository.descontarAsiento`), in ascending ID order. Do not replace it with read-then-save: concurrent sales would oversell.
- `operacionId` (UUID) is an idempotency key persisted by migration `V2__create_venta.sql`. Repeating it with the same flights returns the original sale with 200 and sells nothing; with different flights it returns 409, as does a concurrent request still holding the same key. Sold flights are stored by ID without a foreign key, so flight deletion is unchanged.

## Package purchases

- `POST /api/compras` with `{ paqueteId, operacionId }` buys one package for the authenticated COMPRADOR (owner taken from the JWT) and returns `{ id, operacionId, precio, fecha, paquete }`, with `paquete` in the catalog shape. `precio` copies the package price at purchase time; later price changes do not alter it.
- Frontend: only COMPRADOR cards show "Comprar", which opens `CompraModal` (shared `components/Modal.tsx` dialog) from Inicio, resultados and Mis favoritos; `CompradorPage` owns one `useCompra` state. `api/compras.ts` exposes `iniciar(paqueteId)`, which creates an attempt with its `operacionId` (`crypto.randomUUID()`, so it needs HTTPS or localhost); every confirmation and retry of that attempt sends the same key, and reopening the dialog starts a new one. 409 and 503 show specific messages in the dialog; success shows the price paid returned by the backend.
- `GET /api/compras` returns the authenticated COMPRADOR's purchases (owner from the JWT, COMPRADOR only), newest first (date, then id), in the same `{ id, operacionId, precio, fecha, paquete }` shape as the POST. `paquete` carries the current package data while `precio` is the price paid.
- Frontend history: Mis compras (`ComprasPage`) groups purchases by package (`agruparCompras`, groups ordered by their latest purchase) in `CompraGrupoCard`, with the total paid and a collapsible detail of each purchase's date and amount. `useCompras`, owned by `CompradorPage`, holds the list and the per-package count; package cards show a "Comprado N veces" badge from it. A confirmed purchase is added to the list without reloading, never twice for the same id. The count is computed in the frontend; the backend has no aggregate endpoint. Cards receive their purchase props (`comprados`, `comprar`) through one `CompraControls` object (`types/compra.ts`).
- The frontend generates `operacionId` (UUID) per purchase attempt. The backend sells `[vueloIdaId, vueloVueltaId]` through `VueloClient.vender` (`POST /vuelos/venta` with the same `operacionId`) and then stores the `Compra`. If storing fails, retrying the same key gets the original sale back from flying-service without selling another seat.
- Responses: 201 new purchase; 200 when the same buyer repeats the key for the same package (flying-service is not called again); 409 when the key belongs to another buyer or package, or flying-service rejects the sale with 404/409 (missing flight, no availability); 503 when flying-service is unreachable or answers any other error (other 4xx are integration failures); 404 missing package; 400 missing/invalid fields. Nothing is stored on failure.
- The buyer's `usuario` row is locked for the whole purchase (including the flying-service call) so duplicate submissions serialize. Migration `V6__add_operacion_id_to_compra.sql` makes `compra.operacion_id` unique and required.

## Observability

Optional stack in `docker-compose.observability.yml` (config under `observability/`): `docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build --watch`. Grafana at `http://localhost:3000` (anonymous admin, dev only) with provisioned Prometheus and Loki datasources and the "AterrizAR - Overview" dashboard (folder AterrizAR; JSON in `observability/grafana/provisioning/dashboards/json/`, editable in the UI but UI edits are not written back to the file; export the JSON and commit it to keep them).

- Both services include Actuator and `micrometer-registry-prometheus`; only `/actuator/health` and `/actuator/prometheus` are exposed (public in the backend's `SecurityConfig`, so restrict them at network level outside local use). Prometheus scrapes both; every series carries the `application` tag.
- Logs: the override sets `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` (JSON on stdout); Alloy reads the Compose containers' stdout into Loki and promotes `log.level` to a label. Without the override logs stay human-readable.
- Business metrics (`NegocioMetrics` in the backend, `VentaMetrics` in flying-service) use enum-valued `resultado` tags only: `aterrizar_registros_total`, `aterrizar_logins_total`, `aterrizar_sesiones_refresh_total`, `aterrizar_compras_total`, `aterrizar_compras_monto_total`, gauge `aterrizar_usuarios` (DB count per scrape), `aterrizar_ventas_vuelos_total`, `aterrizar_asientos_vendidos_total`. Error rate comes from Spring's `http_server_requests_seconds` (`status` tag).
- Log hygiene: log internal IDs (usuarioId, compraId, paqueteId, operacionId) and roles; never emails, names, passwords, tokens or cookies. Failed logins deliberately log no identifier.

## Package ratings

- A COMPRADOR may optionally rate each of their purchases once, independently of the purchase transaction, with an integer score from 0 to 10 and an optional comment of at most 255 characters. Repeated purchases of the same package can each have a rating. `PUT /api/compras/{compraId}/valoracion` creates or updates it (200); `DELETE` removes it (204, also when already absent). Ownership is resolved from the signed JWT and persisted buyer profile; missing or foreign purchases return 404, other roles 403, unauthenticated requests 401 and invalid input 400. Writes lock the user's row and the database enforces a unique `compra_id`. Decimal scores are rejected rather than truncated by JSON deserialization.
- `GET /api/valoraciones/mias` is buyer-only and returns `{compraId, valoracion}` items. Public `GET /api/paquetes/{paqueteId}/valoraciones` returns `{promedio, total, valoraciones}` ordered newest first by creation date and id; average is null when empty, includes zero scores and reflects edits/deletions. Each public item has only `{id, puntaje, comentario, fecha}`; buyer names, account data and purchase identifiers are never exposed. Editing preserves the original creation date.
- Migration `V7__valoracion_por_compra.sql` adds the purchase link, creation date, uniqueness, score check and package/date index, and makes comments optional. Historical ratings are preserved without assigning an arbitrary purchase; their purchase link remains null and their date defaults to the migration timestamp. Only new ratings can be managed through purchase endpoints. Existing buyer/package deletion restrictions stay in place.
- Frontend: Mis compras exposes rating controls in each purchase's expandable detail; after purchase confirmation a notice points there. `api/valoraciones.ts` separates public reads from authenticated buyer operations. `useValoraciones` shares confirmed state across purchase rows, while `ValoracionForm` reuses the existing modal and async-action hook. Failed saves preserve the form and the purchase. Package detail shows anonymous ratings, dates and average to every role and visitors, with independent loading/error/retry states.
- Run `npm run test:valoraciones` from `front/` for browser behavior tests using preinstalled Chrome and Vite port 15177. They cover per-purchase ratings, editing, removal, anonymity, mobile layout, retries and purchase independence; they introduce no Gherkin scenarios. Backend tests cover permissions, persisted role checks, validation, concurrency and migration constraints through the existing test setup.

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

## Kubernetes (local cluster)

`k8s/` holds plain manifests applied with `kubectl apply -k k8s/` into namespace `aterrizar`; setup, image builds and the demo script are in `Docs/kubernetes.md`. It targets Docker Desktop Kubernetes and does not replace Compose for development. Cloud deployment, deploy from CD and the observability stack are not part of it.

- Images are built locally as `aterrizar-backend:k8s`, `aterrizar-flying-service:k8s` and `aterrizar-front:k8s` (`imagePullPolicy: IfNotPresent`). The front must be built with `--build-arg VITE_API_URL=http://localhost`: the Ingress serves `/api` (backend) and `/` (front) on the same origin, so `CORS_ALLOWED_ORIGIN` is `http://localhost`.
- Both PostgreSQL databases are StatefulSets with their own volume claim; the services stay Deployments. `backend` has no `replicas` field because its HPA (1 to 3, 80% CPU, needs metrics-server) owns it; it starts with one pod so the dev seeders do not race on an empty database.
- The backend waits for PostgreSQL and flying-service in initContainers; without flying-service `DevPaquetesSeeder` would skip the demo packages.
- Probes use `/actuator/health/liveness` and `/actuator/health/readiness`; `SecurityConfig` permits `/actuator/health/**` for that. `/actuator` is not routed by the Ingress.
- The namespace has a ResourceQuota, so every container and initContainer must declare requests and limits or its pod is rejected.
- The Ingress needs the ingress-nginx controller (archived upstream in 2026, kept because it is the one used in the course). Credentials are not committed: Kustomize generates the `aterrizar-secrets` Secret from the git-ignored `k8s/secrets.env` (copy `k8s/secrets.env.example`), because GitGuardian fails the PR on literal passwords in manifests. Database passwords are fixed when the volume is created, so changing them requires recreating the namespace.

## Build and verification commands

Run the smallest relevant checks first, then the complete checks for each touched application.

### Frontend

```bash
cd front
npm ci
npm run lint
npm test
npm run build
npm run test:admin
npm run test:paquete-detalle
npm run test:bdd
```

Gherkin tests require preinstalled Chrome and port 15174 to be free. See `Docs/gherkin.md` for scope and reports.

`test:paquete-detalle` uses Node's test runner, the existing Playwright dependency and preinstalled Chrome, with Vite on port 15175. It intercepts every API request to verify public detail URLs, reload/back/forward, retained search filters, mobile layout, photo fallback, errors and buyer actions; no backend or browser download is required. It is separate from `npm test` and the six authentication Gherkin scenarios.

Use `npm run dev` for local Vite development and `npm run preview` to inspect a production build.

`npm run test:admin` verifies that role selection remains available at creation and is disabled when editing every account type, including the current administrator; personal data and agent agency changes remain editable. It uses the existing Playwright dependency, preinstalled Chrome and Vite on port 15178, with simulated API responses.

### Application backend

```bash
cd backend
./gradlew test
./gradlew build
```

On Windows without a POSIX shell, use `gradlew.bat` instead of `./gradlew`.

Backend architecture tests use ArchUnit with the existing JUnit runner in `architecture/ArchitectureTest.kt`. They inspect production bytecode without starting Spring, a database, Docker or HTTP services. Run only these checks with `gradlew.bat test --tests "com.aterrizAR.backend.architecture.ArchitectureTest"` from `backend/`; they also run with the normal `test`/`build` tasks. Rules protect layer dependencies, model isolation and JPA repository contracts, and prohibit field injection with `@Autowired`. Only the documented `PingController -> PingLogRepository` and `PingController -> RestTemplate` dependencies are exempted; new controllers must follow the normal boundaries.

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
