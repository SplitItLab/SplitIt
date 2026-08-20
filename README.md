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

En Docker, Compose pisa `POSTGRES_HOST` a `db` (el nombre del servicio). En local, dejalo en `localhost`.

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

---

Laboratorio II · Universidad Austral
