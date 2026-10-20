Desarrolladores:
- Juan Manuel Sanchez Diaz
- Elias Baron
- Diego Ivan Kippes

Frontend: React.TS + Tailwind
Backend: Kotlin + Java 21 + Spring Boot
Servicio externo: Kotlin + Java 21 + Spring Boot
Seguridad: JWT
Database: PostgreSQL
Herramientas:
- Trello: https://trello.com/b/2qZCDHsN/pr%C3%A1cticas-de-desarrollo-de-software
- Docker
- Datadog
- Grafana
- Github actions
- SonarQube Cloud: análisis estático y Quality Gate, con cobertura JaCoCo en ambos servicios. Configuración: [Docs/sonar.md](Docs/sonar.md).
- AWS? 

Enunciado: https://docs.google.com/document/d/1n9sqzswbg9A0U5-oUGCUQGDazKaM0rrPJGt78icD9zI/edit?tab=t.0

## Checkpoints académicos

Las tareas marcadas con `[x]` tienen implementación o evidencia en el repositorio. Las tareas con `[ ]` están pendientes, incompletas o requieren confirmación, según la aclaración de cada ítem.

### Checkpoint 1

- [x] Grupo armado: integrantes listados al inicio de este README.
- [x] Setup básico del frontend: React, TypeScript, Vite y Tailwind.
- [x] Setup básico del backend: aplicación Spring Boot y servicio de vuelos.
- [x] MER / diseño de objetos: [MER](Docs/Diagrama-MER.png) y [diagrama de clases](Docs/ClassDiagram.svg).

### Checkpoint 2

Aplicación y documentación:

- [x] Enunciado: enlace disponible al inicio del README.
- [x] Stack tecnológico: definido y documentado.
- [x] Front: estructura inicial, Home, registro, login y manejo de sesión.
- [x] Back: autenticación, autorización por roles, usuarios, agencias, hoteles, paquetes e integración con vuelos.
- [x] MER / diagrama de clases: disponibles en `Docs/`.
- [ ] App: sección Admin. Home con ABM de usuarios y agencias implementada; las consultas de favoritos, compras, valoraciones y reportes corresponden a tareas pendientes.
- [ ] App: sección Usuario común. Home, autenticación y favoritos implementados; faltan los flujos de búsqueda, compra y reseñas.
- [x] Documentación (README): stack, ejecución local, pruebas y calidad de código.
- [x] Swagger: documentación OpenAPI configurada en los servicios.
- [x] Docker: Dockerfiles del frontend, backend y servicio de vuelos.
- [x] Docker Compose: configuración para levantar las aplicaciones y sus bases de datos.

Repositorio y automatización:

- [x] Git: repositorio e historial de cambios.
- [x] Issues: confirmar el uso de Issues de GitHub para el seguimiento de tareas; el README actualmente enlaza Trello.
- [x] Code Review / branch (MR/PR): trabajo con ramas y Pull Requests, con merges registrados en el historial.
- [x] C.I.: workflows de lint, tests, build y construcción de imágenes.
- [x] Calidad de código (Sonar): configuración y workflow de SonarQube Cloud.
- [ ] Quality Gate en cada PR: implementado para PR internos hacia `develop`; no cubre PR hacia otras ramas ni desde forks.
- [ ] C.Delivery → imagen en DockerHub: CI construye imágenes con `push: false`; falta su publicación.

Pruebas:

- [x] Test unitario: pruebas del mapeo de roles (`ProfileRoleTest`) y de funciones de autenticación del frontend con HTTP simulado.
- [x] Test de interfaz / E2E: escenarios Gherkin de registro y login ejecutados con Playwright. Prueban la interfaz con API simulada; no son E2E con backend real.
- [x] Test de integración: pruebas Spring Boot con MockMvc, servicios y repositorios reales usando H2. No verifican PostgreSQL/Flyway ni ambos servicios ejecutándose juntos.

### Checkpoint 3

Requisitos todavía no informados por la cátedra.

## Cómo levantar el proyecto (Docker - dev)

Requisito: Docker Desktop. Compose incluye valores predeterminados para desarrollo; para personalizarlos, copiá el archivo de ejemplo:
```bash
cp .env.example .env
```

```bash
docker compose up --build --watch
```

Servicios:
- Frontend: http://localhost:5173
- Backend health: http://localhost:8080/api/health
- Backend E2E ping: `POST http://localhost:8080/api/ping`
- Flying Service: http://localhost:8081/ping
- Postgres: localhost:5432 (db: aterrizar_db, user: aterrizar)

Compose Watch reinicia Backend o Flying Service cuando cambia su `src/main` o `build.gradle.kts`; Gradle recompila al arrancar el servicio. Sólo necesitás `--build` nuevamente si tocás un `Dockerfile` o `package.json`.

## Administración y datos demo

Al iniciar con el perfil `dev` (predeterminado de Compose), se cargan automáticamente en PostgreSQL estas cuentas, todas con contraseña `12345678`:

| Cuenta | Rol | Agencia |
| --- | --- | --- |
| `admin@gmail.com` | ADMINISTRADOR | — |
| `usuario@gmail.com` | COMPRADOR | — |
| `agente@gmail.com` | AGENTE | Viajes del Sur |
| `agente.andino@gmail.com` | AGENTE | Rumbo Andino |

También se carga Horizonte Turismo, inicialmente sin agentes. La carga conserva los registros existentes por correo y no duplica datos al reiniciar; si se elimina un registro demo, se vuelve a crear al arrancar. Está deshabilitada fuera de `dev`.

Inicio muestra todos los paquetes con destino, descripción breve e imagen placeholder local, también sin iniciar sesión. `GET /api/paquetes` y `GET /api/paquetes/buscar` son públicos, con el buscador por filtros, orden y paginación (la interfaz todavía no lo consume); el listado de agencias (`GET /api/agencias`) también es público. El detalle de agencias, hoteles y paquetes y las escrituras conservan sus permisos. El administrador accede a Usuarios y Agencias mediante Administración en la navegación, e Inicio vuelve al catálogo. El ABM incluye búsqueda, alta, edición y confirmación de baja. Usuarios incluye filtro por rol, detalle y selección de agencia para agentes. La contraseña de edición es opcional. El backend rechaza la baja de registros con referencias y las operaciones sobre el propio administrador que lo dejarían sin acceso. Una agencia eliminada también elimina sus paquetes si ninguna referencia impide la operación.

En `dev` también se crean hoteles y tres paquetes demo: Bariloche y Córdoba (Viajes del Sur), y Madrid (Rumbo Andino), con origen Buenos Aires. Se seleccionan vuelos futuros de ida/vuelta disponibles consultando flying-service y se crean mediante las validaciones de paquetes. Los reinicios conservan los paquetes existentes sin duplicarlos. Si flying-service no está disponible o faltan vuelos para una ruta, se omite esa carga y se reintenta al próximo arranque. Los datos de vuelos existentes deben incluir fechas futuras.

## Paquetes favoritos

Los compradores con sesión iniciada pueden guardar y quitar paquetes mediante el corazón de cada tarjeta. **Mis favoritos** reúne los paquetes guardados, del más reciente al más antiguo, y permite quitarlos. Los visitantes, agentes y administradores no ven estas opciones. Los favoritos pertenecen a cada cuenta y se conservan al cerrar sesión o reiniciar la aplicación.

El backend expone `GET /api/favoritos`, `PUT /api/favoritos/{paqueteId}` y `DELETE /api/favoritos/{paqueteId}`, disponibles únicamente para COMPRADOR. Agregar o quitar repetidamente no duplica registros ni falla por un favorito ya ausente. La lista muestra los datos actuales del paquete; guardar no reserva vuelos ni congela precios.

Flyway crea la tabla `favorito` mediante `V5__create_favorito.sql`. Eliminar un paquete, incluso por baja de su agencia, limpia sus referencias de favoritos. Eliminar un comprador o cambiarle el rol limpia sus favoritos; las restricciones existentes por compras o valoraciones siguen aplicándose.

La interfaz contempla carga, errores, reintentos y lista vacía. Se reutilizan el transporte autenticado, la renovación de sesión y las tarjetas del catálogo. Las pruebas de contrato están en `front/tests/favoritos.test.mjs`; las de comportamiento, aislamiento entre compradores, permisos y bajas están en `backend/.../favorito/FavoritoControllerTest.kt`.

## Tests Gherkin del frontend

Requiere Node y Chrome instalado. La suite Gherkin cubre únicamente registro y login, con sus seis escenarios existentes. Levanta Vite automáticamente y simula las respuestas de la API; no requiere backend ni Docker.

Desde la raíz del proyecto:

```powershell
cd front
npm ci # Solo si todavía no instalaste las dependencias
npm run test:bdd
```

Para ver Chrome y avanzar acción por acción en el inspector de Playwright:

```powershell
npm run test:bdd:debug
```

Usá **Step over** para avanzar una acción o **Resume** para continuar. Playwright controla el navegador sin mover el cursor físico del mouse.

Más detalles y escenarios en [Docs/gherkin.md](Docs/gherkin.md).

## Calidad de código (Sonar)

El panel del proyecto está en [SonarQube Cloud — TP-PDES](https://sonarcloud.io/project/overview?id=dkippes_TP-PDES). Allí se pueden consultar los problemas detectados, la cobertura y el resultado del Quality Gate.

Sonar está integrado en GitHub Actions mediante el workflow [Code Quality](.github/workflows/sonar.yml). Se ejecuta automáticamente al hacer push a `develop` y al abrir o actualizar un PR dirigido a esa rama. El check `Sonar Quality Gate` ejecuta los tests, genera cobertura JaCoCo de ambos servicios, analiza backend, flying-service y frontend y falla si no se cumple el Quality Gate.

Las ejecuciones se pueden ver en [GitHub Actions — Code Quality](https://github.com/dkippes/TP-PDES/actions/workflows/sonar.yml). La integración requiere el repository secret `SONAR_TOKEN` y Automatic Analysis desactivado en SonarCloud.

Se exige **80% de cobertura total de líneas en backend y flying-service, cada uno por separado**, mediante JaCoCo. `check`, `build` y CI fallan si no se cumple. El frontend queda fuera del cálculo de cobertura por ahora, pero mantiene análisis estático, lint, tests y build.

Después de subir estos archivos y completar el primer análisis, se puede exigir `Sonar Quality Gate` en la protección de `develop` para bloquear merges que no cumplan los criterios de calidad. Más detalles en [Docs/sonar.md](Docs/sonar.md)
