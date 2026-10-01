Desarrolladores:
- Juan Manuel Sanchez Diaz
- Elias Baron
- Diego Ivan Kippes

Frontend: React.TS + Tailwind
Backend: Kotlin +17
Servicio externo: Kotlin +17
Seguridad: JWT
Database: Postgress
Herramientas:
- Trello: https://trello.com/b/2qZCDHsN/pr%C3%A1cticas-de-desarrollo-de-software
- Docker
- Datadog
- Grafana
- Github actions
- SonarQube Cloud: análisis estático y Quality Gate, con cobertura JaCoCo en ambos servicios. Configuración: [Docs/sonar.md](Docs/sonar.md).
- AWS? 

Enunciado: https://docs.google.com/document/d/1n9sqzswbg9A0U5-oUGCUQGDazKaM0rrPJGt78icD9zI/edit?tab=t.0

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

## Calidad de código (Sonar)

El panel del proyecto está en [SonarQube Cloud — TP-PDES](https://sonarcloud.io/project/overview?id=dkippes_TP-PDES). Allí se pueden consultar los problemas detectados, la cobertura y el resultado del Quality Gate.

Sonar está integrado en GitHub Actions mediante el workflow [Code Quality](.github/workflows/sonar.yml). Se ejecuta automáticamente al hacer push a `develop` y al abrir o actualizar un PR dirigido a esa rama. El check `Sonar Quality Gate` ejecuta los tests, genera cobertura JaCoCo de ambos servicios, analiza backend, flying-service y frontend y falla si no se cumple el Quality Gate.

Las ejecuciones se pueden ver en [GitHub Actions — Code Quality](https://github.com/dkippes/TP-PDES/actions/workflows/sonar.yml). La integración requiere el repository secret `SONAR_TOKEN` y Automatic Analysis desactivado en SonarCloud.

Se exige **80% de cobertura total de líneas en backend y flying-service, cada uno por separado**, mediante JaCoCo. `check`, `build` y CI fallan si no se cumple. El frontend queda fuera del cálculo de cobertura por ahora, pero mantiene análisis estático, lint, tests y build.

Después de subir estos archivos y completar el primer análisis, se puede exigir `Sonar Quality Gate` en la protección de `develop` para bloquear merges que no cumplan los criterios de calidad. Más detalles en [Docs/sonar.md](Docs/sonar.md).
