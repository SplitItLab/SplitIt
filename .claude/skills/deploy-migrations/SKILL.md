---
name: deploy-migrations
description: Use when modifying JPA entities, altering the database schema, creating or updating Flyway migrations, or deploying database changes to PostgreSQL/Neon.
---

# Database migrations

The Spring Boot API uses PostgreSQL in production. Every database schema change must include a Flyway migration in the same change.

## Required workflow

- Before modifying a JPA entity, inspect:
  - The current JPA entities.
  - The existing database schema.
  - Every file in `api/src/main/resources/db/migration`.
  - The Flyway and Hibernate configuration.
- Never rely on Hibernate to create or update the production schema.
- Production must use `spring.jpa.hibernate.ddl-auto=validate`.
- Never use `update`, `create`, `create-drop`, or `none` in production.
- Flyway must apply migrations before Hibernate validates the resulting schema.
- Keep every migration compatible with PostgreSQL and Neon.
- Preserve existing production data.
- A new `NOT NULL` column on a populated table must use a safe default or be backfilled before adding the constraint.
- Verify that the application starts against a migrated PostgreSQL database.

## First database migration

If Flyway is not configured and no migrations exist:

1. Add these dependencies to the Spring Boot application:

   ```kotlin
   implementation("org.flywaydb:flyway-core")
   implementation("org.flywaydb:flyway-database-postgresql")
   ```

2. Configure Flyway:

   ```yaml
   spring:
     flyway:
       enabled: true
       locations: classpath:db/migration
       baseline-on-migrate: ${FLYWAY_BASELINE_ON_MIGRATE:false}
       baseline-version: 0

     jpa:
       hibernate:
         ddl-auto: validate
   ```

3. Create:

   ```text
   api/src/main/resources/db/migration/V1__initial_schema.sql
   ```

4. `V1__initial_schema.sql` must describe the complete current database schema, not only the table being added. It must include every existing table, column, constraint, foreign key, and index represented by the JPA entities.

5. If production already contains tables but has no `flyway_schema_history` table:
   - Keep `baseline-on-migrate` disabled by default.
   - Use `FLYWAY_BASELINE_ON_MIGRATE=true` only for the first production deployment.
   - Baseline at version `0` so Flyway still executes `V1__initial_schema.sql`.
   - The initial migration may use `CREATE TABLE IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS` to adopt the existing schema safely.
   - Remove the environment variable or set it to `false` immediately after the first successful deployment.
   - Never leave automatic baselining permanently enabled.

6. If the database is empty, do not enable `baseline-on-migrate`. Flyway must execute `V1__initial_schema.sql` normally.

7. Before deployment, compare the existing production schema with `V1__initial_schema.sql`. Do not guess the existing column types, constraints, or indexes.

## Subsequent migrations

- Create the next migration using:

  ```text
  V<next_version>__<short_description>.sql
  ```

- Never edit, rename, or delete a migration that may already have been applied.
- Add a new forward-only migration instead.
- Subsequent migrations must not use `IF NOT EXISTS` to hide schema drift.
- Include every schema change required by the corresponding JPA change.
- Flyway migration and JPA entity changes must be delivered together.
