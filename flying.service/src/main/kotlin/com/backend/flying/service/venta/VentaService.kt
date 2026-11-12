package com.backend.flying.service.venta

import com.backend.flying.service.observability.ResultadoVenta
import com.backend.flying.service.observability.VentaMetrics
import com.backend.flying.service.vuelo.VueloRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

data class VentaRegistrada(val venta: Venta, val nueva: Boolean)

@Service
class VentaService(
    private val ventaRepository: VentaRepository,
    private val vueloRepository: VueloRepository,
    private val metrics: VentaMetrics,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Sells one seat on every requested flight, all or nothing.
     * A repeated operacionId returns the original sale without selling again.
     */
    @Transactional
    fun vender(request: VentaRequestDTO): VentaRegistrada {
        validateVueloIds(request.vueloIds)
        val vueloIds = request.vueloIds.sorted()

        ventaRepository.findByOperacionId(request.operacionId)?.let { return repetir(it, vueloIds) }

        val venta = registrar(Venta(operacionId = request.operacionId, vueloIds = vueloIds))
        // Ascending order keeps row locks consistent across concurrent sales.
        vueloIds.forEach(::descontarAsiento)
        metrics.venta(ResultadoVenta.EXITOSA, vueloIds.size)
        log.info("Venta registrada: operacionId={} vueloIds={}", venta.operacionId, vueloIds)
        return VentaRegistrada(venta, nueva = true)
    }

    private fun validateVueloIds(vueloIds: List<Long>) {
        if (vueloIds.any { it <= 0 }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Los ids de vuelo deben ser positivos")
        }
        if (vueloIds.toSet().size != vueloIds.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Los ids de vuelo no pueden repetirse")
        }
    }

    private fun repetir(venta: Venta, vueloIds: List<Long>): VentaRegistrada {
        if (venta.vueloIds.sorted() != vueloIds) {
            metrics.venta(ResultadoVenta.CONFLICTO)
            log.warn("Venta rechazada: operacionId={} ya registrada con otros vuelos", venta.operacionId)
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "La operación ${venta.operacionId} ya fue registrada con otros vuelos",
            )
        }
        metrics.venta(ResultadoVenta.REPETIDA)
        log.info("Venta repetida (idempotente): operacionId={}", venta.operacionId)
        return VentaRegistrada(venta, nueva = false)
    }

    private fun registrar(venta: Venta): Venta =
        try {
            ventaRepository.saveAndFlush(venta)
        } catch (_: DataIntegrityViolationException) {
            // Another request with the same operacionId committed first; a retry returns its result.
            metrics.venta(ResultadoVenta.CONFLICTO)
            log.warn("Venta en conflicto: operacionId={} se está procesando", venta.operacionId)
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "La operación ${venta.operacionId} se está procesando, reintente",
            )
        }

    private fun descontarAsiento(vueloId: Long) {
        if (vueloRepository.descontarAsiento(vueloId) > 0) return
        if (!vueloRepository.existsById(vueloId)) {
            metrics.venta(ResultadoVenta.VUELO_INEXISTENTE)
            log.warn("Venta rechazada: vueloId={} inexistente", vueloId)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vuelo $vueloId no encontrado")
        }
        metrics.venta(ResultadoVenta.SIN_DISPONIBILIDAD)
        log.warn("Venta rechazada: vueloId={} sin disponibilidad", vueloId)
        throw ResponseStatusException(HttpStatus.CONFLICT, "Vuelo $vueloId sin disponibilidad")
    }
}
