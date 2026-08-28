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

Requisitos: Docker Desktop y copiar envs
```bash
cp .env.example .env
```

```bash
docker compose up --build # primera vez (baja imágenes y compila) en adelante, con el up corriendo, editás y se recarga solo
docker compose up
```

Servicios:
- Frontend: http://localhost:5173
- Backend: http://localhost:8080/api/ping
- Flying Service: http://localhost:8081/ping
- Postgres: localhost:5432 (db: aterrizar_db, user: aterrizar)

Solo necesitás `--build` si tocás `build.gradle.kts`, `Dockerfile` o `package.json`.
