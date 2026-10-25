# ADR: Motor de cálculo de KPIs

Status: Aprobado

Fecha: 2026-10-10

Issue: SPT-77

Epic: EPICA-05 — Cross-Project KPI Reporting

Relacionado: SPT-78 (envío y programación — fuera de este ADR)

## Contexto

Necesitamos un componente que, al ejecutarse, calcule los 7 KPIs definidos en SPT-77 y los deje disponibles como una lista de valores.

El motor no se ocupa de:

- a dónde se envían o persisten los valores (SPT-78),
- cuándo o cada cuánto se ejecuta (SPT-78).

La instrumentación de requests entrantes sí está: Micrometer observa cada request HTTP y deja las muestras en los puertos de latencia y de tasa 5xx. El cálculo de cada KPI no intercepta nada; solo lee el puerto.

Los KPIs son de naturaleza distinta (negocio, performance, salud del sistema). El diseño debe permitir agregar un octavo KPI sin modificar el código de los 7 existentes.

## Decisión

Implementar un motor de cálculo extensible basado en estrategia + inyección de dependencias de Spring.

### Componentes principales

`Kpi` (interfaz): cada KPI implementa esta interfaz.

- `val id: String`
- `fun calculate(context: KpiCalculationContext): KpiValue?`

`KpiEngine`: recibe `List<Kpi>` por constructor (Spring inyecta todos los beans que implementan `Kpi`).

- Método principal: `fun calculate(context: KpiCalculationContext): List<KpiResult>`
- Recorre la lista, llama a cada KPI, captura excepciones, loguea y continúa.
- Si un KPI falla → su resultado lleva `value = null`.
- El cálculo de cada KPI es puro (sin side effects). El engine no registra la corrida: desde SPT-87 la graba `RunKpiReport`, después del envío.

`KpiCalculationContext`: data class inmutable con el rango de fechas sobre el que calcular. El engine lo recibe como parámetro y se lo pasa a cada KPI.

`KpiResult`: data class inmutable con `id`, `value` (`Double?` o `String?`), `calculatedAt`.

### Registro de KPIs

Cada KPI es un `@Component` (o `@Bean`). Spring arma automáticamente la `List<Kpi>`.

Agregar un nuevo KPI = crear una clase nueva que implemente `Kpi` y anotarla. Cero cambios en el engine ni en los KPIs existentes.

### Puertos e implementaciones en este issue

| KPI | Fuente | Decisión en este ticket |
| --- | --- | --- |
| Eventos creados por día | Repositorios/services existentes (misma base) | Implementación real |
| Gastos registrados por día | Repositorios/services existentes | Implementación real |
| % eventos con ≥1 gasto | Repositorios/services existentes | Implementación real |
| Latencia p95 POST /expenses | Puerto `LatencyMetricsPort` | `JpaRequestMetrics` (tabla `request_sample`, SPT-87), alimentado por las observaciones de Micrometer |
| Tasa error 5xx últimas 24h | Puerto `ErrorRatePort` | La misma fuente. 4xx no cuenta como error. Null si no hay muestras en la ventana |
| Health check backend | `/api/status` existente (devuelve OK) | Llamada o chequeo equivalente |
| Última ejecución exitosa del job | Puerto `LastRunPort` | `JpaLastRun` (tabla `kpi_run`, SPT-87) |

Puertos definidos (segregados, ISP):

```kotlin
interface LatencyMetricsPort {
    fun p95(endpoint: String, window: ClosedRange<Instant>): Duration?
}

interface ErrorRatePort {
    fun rate5xx(window: ClosedRange<Instant>): Double? // contrato: 4xx no cuenta como error
}

interface LastRunPort {
    fun recordRun(reportDay: LocalDate, ranAt: Instant, status: KpiRunStatus) // desde SPT-87
    fun lastSuccessfulRun(): Instant?
}
```

Cada KPI depende únicamente del puerto que necesita. Desde SPT-87 el last-run se guarda en `kpi_run` y sobrevive a los reinicios.

Los requests entrantes se observan con Micrometer (`spring-boot-micrometer-metrics`). `ServerRequestMetricsHandler` implementa `ObservationHandler` sobre `ServerRequestObservationContext`. Al terminar cada request guarda método, ruta, status y duración en la tabla `request_sample` (`JpaRequestMetrics`, SPT-87). Los timers de Micrometer no responden “p95 de esta ventana”, así que los puertos leen esa tabla (retención de 7 días). Una muestra con status menor a 100 no se guarda. 4xx entra en el total y no suma como error.

### Manejo de errores

El engine no falla completo si un KPI lanza excepción. Loguea el error y emite el resultado con `value = null`. Los demás KPIs se calculan normalmente.

### Output

Lista de `KpiResult`. No hay persistencia ni envío. El consumidor (futuro SPT-78) recibe esta lista.

## Alternativas consideradas

Engine monolítico con `when`/`switch` sobre un enum de KPIs.

Descartado: agregar un KPI obliga a modificar el engine. Viola el criterio de aceptación.

Calcular todo en un solo servicio sin interfaz `Kpi`.

Descartado: mismo problema de extensibilidad y mezcla de responsabilidades.

Instrumentar requests a mano, con histogramas propios.

Descartado. Se usa Micrometer, que ya observa los requests del servidor. Los puertos `LatencyMetricsPort` y `ErrorRatePort` quedan como contrato. La tabla de muestras existe solo porque el timer de Micrometer no está partido por ventana.

Timestamp de última corrida como campo mutable en el engine.

Descartado a favor de `LastRunPort`: más fácil de reemplazar por una implementación persistente sin tocar el engine ni el KPI 7.

Un único `RequestMetricsPort` con ambos métodos.

Descartado: viola Interface Segregation. Los KPIs de latencia y de error rate no deben depender de métodos que no usan.

## Consecuencias

El motor queda desacoplado del destino de los datos.

Core de cálculo puro e inmutable: `KpiCalculationContext` y `KpiResult` son data classes inmutables; los KPIs no tienen side effects. La corrida se registra fuera del engine, después del cálculo y del envío (SPT-87).

Agregar KPIs futuros es barato y no requiere tocar código existente (cumple criterio de aceptación + test del 8vo KPI).

Los KPIs de latencia y de tasa 5xx leen muestras reales de los requests. Sin requests en la ventana, el valor es null. Desde SPT-87 las muestras y el last-run sobreviven a los reinicios.

Se deja este ADR como registro corto de alternativas (cumple criterio de aceptación).

## Fuera de alcance (explícito)

- Envío o persistencia de los valores calculados.
- Programación / cron / job scheduler.
- Instrumentación de llamadas salientes. Las observaciones cubren requests entrantes.

## Criterios de aceptación cubiertos

- Cálculo correcto de los 7 valores contra datos de prueba conocidos.
- Posibilidad de agregar un 8vo KPI sin modificar los anteriores + evidencia (test).
- Registro de alternativas (este ADR).
- Tests por KPI, test del 8vo KPI, test que 4xx no cuenta como error 5xx, y test del puerto de métricas si aplica.

## Actualización (SPT-87)

Las muestras de request y las corridas pasan a Postgres (migración `V3__kpi_persistence.sql`), así que un deploy ya no las borra.

- `request_sample`: una fila por request, con endpoint, status, duración en µs e instante. `JpaRequestMetrics` implementa `LatencyMetricsPort` y `ErrorRatePort`, y calcula el p95 y la tasa de 5xx en SQL.
- El p95 sigue siendo nearest-rank (`ceil(0.95 · n)`), así que el valor no cambia. Se calcula con `ROW_NUMBER()` en lugar de `percentile_disc`: en Postgres dan lo mismo, pero H2, la base de los tests, no implementa `percentile_disc` como nearest-rank.
- Si falla el insert de una muestra, se loguea y la request sigue. Solo se pierde esa muestra.
- Después de cada corrida, `KpiReportJob` borra las muestras de más de 7 días.
- `kpi_run`: una fila por corrida, con `report_day`, `ran_at` y `status` (`sent`, `failed` o `skipped`). El engine ya no registra la corrida: la graba `RunKpiReport` después del envío.
- `last_successful_run` cambia de significado. Antes era "el cálculo terminó"; ahora es el `ran_at` más reciente con `status = 'sent'`. Una corrida `failed` o `skipped` no lo mueve. El detalle por modo está en el ADR de SPT-78.
