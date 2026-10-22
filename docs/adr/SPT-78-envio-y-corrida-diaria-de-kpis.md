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

Spring crea un solo `KpiSnapshotExporter` según `KPI_EXPORT_MODE`. Los dos modos escriben la misma tabla. Cambia el canal:

| Modo | Canal |
| --- | --- |
| `local` (default, y si la variable no está) | `local` |
| `report` | `report` |

El default es `local`. Production de Vercel tiene que setear `KPI_EXPORT_MODE=report`. Preview y local no. Los únicos valores son `local` y `report`. Otro valor hace que la aplicación no arranque.

La corrida la dispara `KpiReportJob`, cron `0 15 3 * * *` en `UTC` (03:15). Llama al caso de uso directo, sin HTTP. No hay POST: cualquier usuario con sesión podría disparar el envío.

`GET /api/kpi-runs` lista solo el canal del exportador activo. Cada corrida agrega una fila. Dos días dejan dos filas. El mismo día, corrido dos veces, deja dos filas de ese día.

La tabla es el destino de esta semana. La semana que viene se agrega otro `KpiExporter` y `KpiExportConfig` lo elige para `report`. `RunKpiReport`, el motor y el canal `local` no se tocan.

## Alternativa descartada

Guardar el modo local en un archivo del contenedor.

Descartado: el backend de prod es un contenedor en Vercel. El disco no conserva la corrida del día siguiente. Postgres (Neon) sí.

## Cómo verificar que los datos llegaron

La corrida se ve en el log del job. No hace falta leer el código.

`GET /api/kpi-runs` devuelve las corridas guardadas, de la más vieja a la más nueva, solo del canal activo. Hace falta sesión. No dispara una corrida.

El cron loguea el resumen y, en una línea por KPI, el id y el valor (`null` si no hubo dato):

```text
KPI run stored. destination={} reportDay={} id={} kpiCount={}
KPI value. id={} value={}
```

En dev, `destination` es `local`. Esta query no devuelve filas:

```sql
SELECT s.id, s.report_day, s.calculated_at, v.kpi_id, v.numeric_value, v.text_value
FROM kpi_snapshot s
JOIN kpi_snapshot_value v ON v.snapshot_id = s.id
WHERE s.channel = 'report'
ORDER BY s.id, v.kpi_id;
```

Las corridas de dev están en `channel = 'local'`.

En prod, con `KPI_EXPORT_MODE=report`, la misma query muestra una fila de valores por KPI de cada corrida, y el log dice `destination=report`.

El archivo `api/http/kpi-runs.http` inicia sesión y consulta el GET. La corrida la hace el job.

## Fuera de alcance

Integrar Dataverse u otra plataforma central. Persistir `LastRunPort`: siguió en memoria, como en SPT-77, hasta SPT-87.

## Actualización (SPT-87)

Cada exportador devuelve en el receipt qué pasó con el envío, y `RunKpiReport` deja una fila por corrida en `kpi_run`:

| Modo | `status` |
| --- | --- |
| `local`, `report` | `sent` cuando la corrida queda guardada en `kpi_snapshot` |
| `supabase` | `sent` si la plataforma aceptó el lote; `failed` si respondió error o no se alcanzó; `skipped` sin claves o sin valores numéricos |

Si el exportador lanza una excepción (por ejemplo, se cae la base en `local` o `report`), la corrida se corta sin dejar fila y el error queda en el log del job.

`last_successful_run` es el `ran_at` más reciente con `status = 'sent'`, y sobrevive a los reinicios. Cada corrida lee ese valor antes de grabar su propia fila, así que reporta la corrida enviada anterior.

Para ver las corridas:

```sql
SELECT report_day, ran_at, status FROM kpi_run ORDER BY id;
```
