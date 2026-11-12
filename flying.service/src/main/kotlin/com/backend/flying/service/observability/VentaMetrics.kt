package com.backend.flying.service.observability

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

enum class ResultadoVenta { EXITOSA, REPETIDA, SIN_DISPONIBILIDAD, VUELO_INEXISTENTE, CONFLICTO }

/** Flight-sale metrics; series are registered up front so dashboards show zero before the first sale. */
@Component
class VentaMetrics(private val registry: MeterRegistry) {
    private val ventas = ResultadoVenta.entries.associateWith {
        Counter.builder("aterrizar.ventas.vuelos").tag("resultado", it.name.lowercase()).register(registry)
    }
    private val asientos = Counter.builder("aterrizar.asientos.vendidos")
        .description("Asientos descontados por ventas exitosas")
        .register(registry)

    fun venta(resultado: ResultadoVenta, asientosVendidos: Int = 0) {
        ventas.getValue(resultado).increment()
        if (asientosVendidos > 0) asientos.increment(asientosVendidos.toDouble())
    }
}
