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
- [x] Back: autenticación, autorización por roles, hoteles, paquetes e integración con vuelos.
- [x] MER / diagrama de clases: disponibles en `Docs/`.
- [ ] App: sección Admin. Hay autorización y endpoints de catálogo; falta la interfaz de administración.
- [ ] App: sección Usuario común. Home y autenticación implementadas; faltan los flujos de búsqueda, favoritos, compra y reseñas.
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

## Tests Gherkin del frontend

Requiere Node y Chrome instalado. Los tests levantan Vite automáticamente y simulan las respuestas de la API; no requieren backend ni Docker.

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

Después de subir estos archivos y completar el primer análisis, se puede exigir `Sonar Quality Gate` en la protección de `develop` para bloquear merges que no cumplan los criterios de calidad. Más detalles en [Docs/sonar.md](Docs/sonar.md).
