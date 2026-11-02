# SplitIt

Monorepo full-stack: frontend en Next.js, API en Spring Boot + Kotlin, y Postgres atrás.

```
┌─────────────┐     /api/*      ┌─────────────┐     JDBC      ┌─────────────┐
│   Next.js   │ ──────────────► │ Spring Boot │ ────────────► │  Postgres   │
│   :3000     │                 │    :8080    │               │    :5432    │
└─────────────┘                 └─────────────┘               └─────────────┘
```

## Stack

| Capa          | Tecnología                                 |
| ------------- | ------------------------------------------ |
| Frontend      | Next.js 16 · React 19 · Tailwind 4 · shadcn |
| Backend       | Spring Boot 4 · Kotlin · Java 21 · JPA     |
| Base de datos | PostgreSQL 15                              |
| Runtime       | Docker Compose                             |

## Estructura

```
SplitIt/
├── web/                 # Next.js
├── api/                 # Spring Boot (Kotlin)
├── hooks/               # git hooks (pre-commit)
├── docker-compose.yml
└── .env.example
```

## Arranque rápido

Necesitás [Docker](https://docs.docker.com/get-docker/) y Docker Compose.

```bash
cp .env.example .env
# completá las variables (JWT_SECRET ≥ 32 caracteres)

docker compose up --build
```

| Servicio  | URL                                     |
| --------- | --------------------------------------- |
| Frontend  | http://localhost:3000                   |
| Backend   | http://localhost:8080                   |
| Postgres  | `localhost:5432` · database `splitit`   |

## Git hooks

Git no versiona lo que vive en `.git/hooks`. Después del clone:

```bash
./hooks/install.sh
```

## Variables de entorno

Copiá `.env.example` y llenalo. Lo mínimo:

```env
POSTGRES_USER=
POSTGRES_PASSWORD=
POSTGRES_DB=splitit
POSTGRES_PORT=5432
POSTGRES_HOST=localhost

NEXT_PUBLIC_API_URL=http://localhost:8080

JWT_SECRET=
JWT_EXPIRATION_HOURS=8
```

`POSTGRES_PORT` es el puerto **en tu máquina**. Postgres adentro del contenedor siempre escucha en `5432`; Compose pisa `POSTGRES_HOST=db` y `POSTGRES_PORT=5432` en el servicio `api` para que no use el puerto del host. Si corrés la API con `./gradlew bootRun`, dejá `POSTGRES_HOST=localhost` y el `POSTGRES_PORT` publicado (por ejemplo `5433` si 5432 ya está ocupado).

## Desarrollo local (sin Docker para todo)

Postgres sí o sí tiene que estar corriendo. Lo más fácil:

```bash
docker compose up db
```

**API**

```bash
cd api
./gradlew bootRun
```

**Web**

```bash
cd web
npm ci
npm run dev
```

## Tests

```bash
cd api
./gradlew test
```

## Despliegue a producción (Azure)

Cada push a `main` con el pipeline en verde despliega solo lo que cambió: el job `deploy` de `api-pipeline.yml` actualiza la Container App `ca-splitit-api` y el de `web-pipeline.yml` el App Service `app-splitit-web`, ambos con la imagen `sha-<commit>` de GHCR. Solo se cambia la imagen; las variables y secretos de Azure no se tocan. Las PR no despliegan (QA y previews siguen en Vercel).

Para volver atrás:

- **API**: en Azure, Container App → *Revisions* → activar la revisión anterior, o `az containerapp update -n ca-splitit-api -g <rg> --image ghcr.io/splititlab/splitit/api:sha-<commit>`.
- **Web**: App Service → *Deployment Center* → poner el tag `sha-<commit>` anterior, o `az webapp config container set -n app-splitit-web -g <rg> --container-image-name ghcr.io/splititlab/splitit/web:sha-<commit>`.

---

Laboratorio II · Universidad Austral
