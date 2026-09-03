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
