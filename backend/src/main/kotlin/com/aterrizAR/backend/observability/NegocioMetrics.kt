package com.aterrizAR.backend.observability

import com.aterrizAR.backend.usuario.UsuarioRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

enum class ResultadoRegistro { EXITOSO, DUPLICADO }
enum class ResultadoLogin { EXITOSO, FALLIDO }
enum class ResultadoRefresh { EXITOSO, INVALIDO }
enum class ResultadoCompra { EXITOSA, REPETIDA, RECHAZADA, SERVICIO_NO_DISPONIBLE, OPERACION_EN_USO }

/**
 * Business metrics. Every series is registered up front so `rate()` queries and dashboards see zero
 * instead of "no data" before the first event. Tags are enum values only: never user-supplied text.
 * HTTP error rate comes from Spring's own `http_server_requests_seconds` (tag `status`).
 */
@Component
class NegocioMetrics(private val registry: MeterRegistry) {
    private val registros = counters("aterrizar.registros", "resultado", ResultadoRegistro.entries) { it.name }
    private val logins = counters("aterrizar.logins", "resultado", ResultadoLogin.entries) { it.name }
    private val refreshes = counters("aterrizar.sesiones.refresh", "resultado", ResultadoRefresh.entries) { it.name }
    private val compras = counters("aterrizar.compras", "resultado", ResultadoCompra.entries) { it.name }
    private val montoVendido = Counter.builder("aterrizar.compras.monto")
        .description("Suma del precio de las compras exitosas")
        .register(registry)

    fun registro(resultado: ResultadoRegistro) = registros.getValue(resultado).increment()
    fun login(resultado: ResultadoLogin) = logins.getValue(resultado).increment()
    fun refresh(resultado: ResultadoRefresh) = refreshes.getValue(resultado).increment()

    fun compra(resultado: ResultadoCompra, precio: Float? = null) {
        compras.getValue(resultado).increment()
        if (resultado == ResultadoCompra.EXITOSA && precio != null) montoVendido.increment(precio.toDouble())
    }

    private fun <E : Enum<E>> counters(name: String, tag: String, values: List<E>, label: (E) -> String) =
        values.associateWith { Counter.builder(name).tag(tag, label(it).lowercase()).register(registry) }
}

/** Gauge of accounts currently stored; read from the database on each scrape (a cheap COUNT). */
@Configuration
class UsuariosGaugeConfig(registry: MeterRegistry, usuarioRepository: UsuarioRepository) {
    init {
        Gauge.builder("aterrizar.usuarios.total", usuarioRepository) { it.count().toDouble() }
            .description("Cuentas de usuario existentes")
            .register(registry)
    }
}
