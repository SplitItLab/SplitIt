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
- cuándo o cada cuánto se ejecuta (SPT-78),
- instrumentación de requests (interceptor, histogramas, Micrometer) — queda para un ticket posterior.

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
- El cálculo de cada KPI es puro (sin side effects). Después de obtener todos los resultados, el engine llama a `LastRunPort.recordSuccessfulRun` (efecto fuera del core de cálculo).

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
| Latencia p95 POST /expenses | Puerto `LatencyMetricsPort` | Implementación stub (sin data real) |
| Tasa error 5xx últimas 24h | Puerto `ErrorRatePort` | Implementación stub (sin data real). El puerto y los tests deben reflejar que 4xx no cuenta como error |
| Health check backend | `/api/status` existente (devuelve OK) | Llamada o chequeo equivalente |
| Última ejecución exitosa del job | Puerto `LastRunPort` | Implementación in-memory por ahora |

Puertos definidos (segregados, ISP):

```kotlin
interface LatencyMetricsPort {
    fun p95(endpoint: String, window: ClosedRange<Instant>): Duration?
}

interface ErrorRatePort {
    fun rate5xx(window: ClosedRange<Instant>): Double? // contrato: 4xx no cuenta como error
}

interface LastRunPort {
    fun recordSuccessfulRun(at: Instant)
    fun lastSuccessfulRun(): Instant?
}
```

Cada KPI depende únicamente del puerto que necesita. Las implementaciones stub/in-memory viven en este ticket. La instrumentación real (interceptor + Micrometer) y la persistencia del last-run quedan fuera de scope.

### Manejo de errores

El engine no falla completo si un KPI lanza excepción. Loguea el error y emite el resultado con `value = null`. Los demás KPIs se calculan normalmente.

### Output

Lista de `KpiResult`. No hay persistencia ni envío. El consumidor (futuro SPT-78) recibe esta lista.

## Alternativas consideradas

Engine monolítico con `when`/`switch` sobre un enum de KPIs.

Descartado: agregar un KPI obliga a modificar el engine. Viola el criterio de aceptación.

Calcular todo en un solo servicio sin interfaz `Kpi`.

Descartado: mismo problema de extensibilidad y mezcla de responsabilidades.

Instrumentar requests a mano (histogramas propios) en este ticket.

Descartado por scope. Se difiere. Micrometer (o equivalente) se evaluará cuando se haga la instrumentación. Los puertos `LatencyMetricsPort` y `ErrorRatePort` dejan el contrato listo.

Timestamp de última corrida como campo mutable en el engine.

Descartado a favor de `LastRunPort`: más fácil de reemplazar por una implementación persistente sin tocar el engine ni el KPI 7.

Un único `RequestMetricsPort` con ambos métodos.

Descartado: viola Interface Segregation. Los KPIs de latencia y de error rate no deben depender de métodos que no usan.

## Consecuencias

El motor queda desacoplado del destino de los datos.

Core de cálculo puro e inmutable: `KpiCalculationContext` y `KpiResult` son data classes inmutables; los KPIs no tienen side effects. El único efecto (`recordSuccessfulRun`) ocurre después del cálculo.

Agregar KPIs futuros es barato y no requiere tocar código existente (cumple criterio de aceptación + test del 8vo KPI).

Los KPIs 4 y 5 no tendrán datos reales hasta que exista la instrumentación de requests. Los tests los cubren contra los puertos stub.

El last-run se pierde al reiniciar el proceso (aceptable en este ticket).

Se deja este ADR como registro corto de alternativas (cumple criterio de aceptación).

## Fuera de alcance (explícito)

- Envío o persistencia de los valores calculados.
- Programación / cron / job scheduler.
- Interceptor de requests e histogramas reales (Micrometer u otro).
- Instrumentación de llamadas salientes.

## Criterios de aceptación cubiertos

- Cálculo correcto de los 7 valores contra datos de prueba conocidos.
- Posibilidad de agregar un 8vo KPI sin modificar los anteriores + evidencia (test).
- Registro de alternativas (este ADR).
- Tests por KPI, test del 8vo KPI, test que 4xx no cuenta como error 5xx, y test del puerto de métricas si aplica.
