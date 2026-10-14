# ADR: Envío de KPIs a Supabase

Status: Aprobado

Fecha: 2026-10-14

Issue: SPT-85

Epic: EPICA-05 — Cross-Project KPI Reporting

Relacionado: SPT-77 (motor), SPT-78 (puerto `KpiExporter` y corrida diaria)

## Contexto

SPT-78 dejó la corrida diaria y dos exportadores que solo escriben en `kpi_snapshot`. La cátedra eligió Supabase como plataforma central. Su contrato es el OpenAPI de `https://lab4-kpis.github.io/kpis/dev/openapi.yaml`.

## Decisión

Un tercer exportador, `SupabaseKpiExporter`, se elige con `KPI_EXPORT_MODE=supabase`. `RunKpiReport`, el motor y los otros dos modos no cambian.

Por cada corrida:

1. `GET /rest/v1/kpi_catalog?select=id` y `POST /rest/v1/kpi_catalog` solo con los KPIs que todavía no están (el catálogo da 409 si el id existe). Así el catálogo de cada ambiente se registra solo, la primera vez.
2. `POST /rest/v1/measurement?on_conflict=project_id,kpi_id,date,env` con `Prefer: resolution=ignore-duplicates`. Un objeto por KPI: `kpi_id`, `date` (día reportado), `env`, `value`, `run_id` (UUID por corrida). Sin `project_id` ni `reported_at`.

Headers: `apikey` (publishable) y `X-Project-Key`. Nunca `Authorization`.

La plataforma conserva el primer valor de cada `(kpi_id, date, env)`: reenviar el mismo día no duplica.

### Mapeo de los 7 KPIs

El mapeo vive solo en el exportador (`SupabaseKpiCatalog`). Los ids del motor no cambian.

| KPI del motor | id en la plataforma | kind | unit | value |
| --- | --- | --- | --- | --- |
| `events-created-per-day` | `events_created_per_day` | business | `{event}` | cantidad |
| `expenses-registered-per-day` | `expenses_registered_per_day` | business | `{expense}` | cantidad |
| `events-with-expense-percent` | `events_with_expense_percent` | business | `%` | 0 a 100 |
| `post-expenses-latency-p95` | `post_expenses_latency_p95` | technical | `ms` | milisegundos |
| `error-rate-5xx-24h` | `error_rate_5xx_24h` | technical | `1` | fracción 0 a 1 |
| `backend-health` | `backend_health` | health | `1` | 1 si `/api/status` dice `OK`, 0 si no |
| `last-successful-run` | `last_successful_run` | health | `s` | segundos desde epoch |

La plataforma exige `value` numérico. Un KPI sin dato (`null`, por ejemplo la latencia sin requests) no se envía ese día; se loguea `KPI value not exported`.

## Configuración

Todo por variable de entorno, como `JWT_SECRET`. Nada va en el repo.

| Variable | Uso |
| --- | --- |
| `KPI_EXPORT_MODE` | `supabase` activa este exportador |
| `KPI_EXPORT_ENV` | `dev` (default), `qa` o `prod`. Va en cada medición y debe ser el ambiente de la project key. Otro valor hace que la aplicación no arranque |
| `SUPABASE_URL` | URL del proyecto Supabase |
| `SUPABASE_PUBLISHABLE_KEY` | clave pública compartida (`apikey`) |
| `SUPABASE_PROJECT_KEY` | clave privada del equipo y ambiente (`X-Project-Key`) |

Para activar prod: `KPI_EXPORT_MODE=supabase`, `KPI_EXPORT_ENV=prod` y cargar `SUPABASE_PROJECT_KEY` de prod en la plataforma de nube. Alcanza con reiniciar el proceso. No hace falta un deploy de código.

## Sin project key

Si falta la project key, la URL o la publishable key, el cálculo de los 7 KPIs corre normal y el envío se saltea con un `WARN`:

```text
KPI export skipped: no prod key configured. reportDay=2026-10-11
```

(`dev` o `qa` en lugar de `prod`, según `KPI_EXPORT_ENV`.) No lanza excepción: el job no se cae.

Si la plataforma responde error o no se alcanza, se loguea `KPI export failed. env=… reportDay=… reason=…` y el job termina. No se loguea ninguna clave. Como el lote es atómico, el reintento es la corrida siguiente o repetir el día.

## Cómo verificar que los datos llegaron

- Log del job: `KPI export sent. env=dev reportDay=… rows=6`, y `KPI catalog registered` la primera vez.
- Catálogo propio (7 filas):

```bash
curl -s "$SUPABASE_URL/rest/v1/kpi_catalog?select=id,kind,unit" \
  -H "apikey: $SUPABASE_PUBLISHABLE_KEY" -H "X-Project-Key: $SUPABASE_PROJECT_KEY"
```

- Mediciones propias del ambiente de la key:

```bash
curl -s "$SUPABASE_URL/rest/v1/measurement?order=reported_at.desc&limit=10" \
  -H "apikey: $SUPABASE_PUBLISHABLE_KEY" -H "X-Project-Key: $SUPABASE_PROJECT_KEY"
```

- En el portal de la cátedra: el panel público de cumplimiento, o "Mi equipo" ingresando la project key. Solo `prod` cuenta para la nota.

Reenviar el mismo día: el `GET` de mediciones sigue mostrando una sola fila por `(kpi_id, date, env)`.

## Consecuencias

`list()` del exportador devuelve vacío: no guarda nada local. `KpiReportJob` loguea igual `KPI run stored … destination=supabase`; el resultado real del envío está en las líneas `KPI export …`.

## Fuera de alcance

Obtener la project key de prod (depende de la cátedra).
