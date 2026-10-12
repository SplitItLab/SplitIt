# ADR: Envío de KPIs y corrida diaria

Status: Aprobado

Fecha: 2026-10-11

Issue: SPT-78

Epic: EPICA-05 — Cross-Project KPI Reporting

Relacionado: SPT-77 (el motor calcula; este ADR guarda y dispara)

## Contexto

SPT-77 deja los 7 KPIs en una lista y no los persiste. Hay que poder disparar esa corrida, en código, y guardar cada día como un registro consultable.

El destino final (Dataverse u otra plataforma) se define la semana que viene. Dev y prod no pueden escribir al mismo destino: si dev también enviara, las métricas reportadas se duplican. La corrida en dev igual tiene que calcular, para probar el motor.

## Decisión

Un caso de uso, `RunKpiReport`, arma la ventana del día en UTC y llama a `KpiEngine.calculate`. No conoce el ambiente. Entrega la lista a un puerto `KpiExporter`.

Spring crea un solo exportador según `KPI_EXPORT_MODE`:

| Modo | Bean | Qué hace esta semana |
| --- | --- | --- |
| `local` (default, y si la variable no está) | `LocalKpiExporter` | Inserta en `kpi_snapshot` con `channel = local`. `delivered = false`. |
| `report` | `ReportKpiExporter` | Inserta en la misma tabla con `channel = report`. `delivered = true`. |

El default es `local`. Production de Vercel tiene que setear `KPI_EXPORT_MODE=report`. Preview y local no. Los únicos valores son `local` y `report`. Otro valor hace que la aplicación no arranque, porque no hay exportador.

La corrida se dispara de dos maneras, las dos en código:

- `POST /api/kpi-runs`, con sesión. Body opcional `{ "reportDay": "2026-10-10" }`. Sin día, usa ayer UTC.
- `KpiReportJob`, cron `0 15 3 * * *` en `UTC` (03:15). Llama al mismo caso de uso. El cron no pasa por HTTP.

`GET /api/kpi-runs` lista solo el canal del exportador activo. Cada corrida agrega una fila. Dos días dejan dos filas. El mismo día, corrido dos veces, deja dos filas de ese día.

La tabla es el destino de esta semana. La semana que viene se reemplaza `ReportKpiExporter`. `RunKpiReport` y el motor no se tocan.

## Alternativa descartada

Guardar el modo local en un archivo del contenedor.

Descartado: el backend de prod es un contenedor en Vercel. El disco no conserva la corrida del día siguiente. Postgres (Neon) sí.

## Cómo verificar que los datos llegaron

Quien dispara la corrida mira la respuesta o el log. No hace falta leer el código.

`POST /api/kpi-runs` responde 201 con `destination`, `delivered`, `reportDay`, `id` y un resultado por KPI. `numericValue` o `textValue` trae el número o el texto. Los dos vienen null si ese KPI no tuvo dato.

`GET /api/kpi-runs` devuelve esas corridas, de la más vieja a la más nueva, solo del canal activo.

El cron loguea el resumen y, en una línea por KPI, el id y el valor (`null` si no hubo dato):

```text
KPI run stored. destination={} delivered={} reportDay={} id={} kpiCount={}
KPI value. id={} value={}
```

En dev, `destination` es `local` y `delivered` es `false`. Esta query no devuelve filas:

```sql
SELECT s.id, s.report_day, s.calculated_at, v.kpi_id, v.numeric_value, v.text_value
FROM kpi_snapshot s
JOIN kpi_snapshot_value v ON v.snapshot_id = s.id
WHERE s.channel = 'report'
ORDER BY s.id, v.kpi_id;
```

Las corridas de dev están en `channel = 'local'`.

En prod, con `KPI_EXPORT_MODE=report`, la misma query muestra una fila de valores por KPI de cada corrida, y el POST dice `destination: "report"`, `delivered: true`.

El archivo `api/http/kpi-runs.http` recorre el camino local: login, dos días distintos, y el GET que los encuentra.

## Fuera de alcance

Integrar Dataverse u otra plataforma central. Persistir `LastRunPort`: sigue en memoria, como en SPT-77.
