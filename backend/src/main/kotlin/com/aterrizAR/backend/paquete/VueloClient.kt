package com.aterrizAR.backend.paquete

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.util.UUID

/**
 * Acceso a flying-service para validar vuelos, vincular los datos demo y vender los asientos de una compra.
 */
@Component
class VueloClient(
    private val restTemplate: RestTemplate,
    @Value("\${flying.service.url:http://flying-service:8081}") private val flyingServiceUrl: String,
) {
    fun findAll(): List<VueloCatalogoDTO> =
        restTemplate.getForObject("$flyingServiceUrl/vuelos", Array<VueloCatalogoDTO>::class.java)?.toList().orEmpty()

    fun findDisponibilidad(vueloId: Long): Int? =
        try {
            restTemplate.getForObject("$flyingServiceUrl/vuelos/$vueloId", VueloResponse::class.java)?.disponibilidad
        } catch (e: RestClientException) {
            null
        }

    /**
     * Availability per flight id in a single request. Flights unknown to flying-service are absent from the map;
     * null means the service did not answer, so callers can tell "unknown" apart from "missing".
     */
    fun findDisponibilidades(vueloIds: Collection<Long>): Map<Long, Int>? {
        val ids = vueloIds.distinct().sorted()
        if (ids.isEmpty()) return emptyMap()
        return try {
            restTemplate.getForObject("$flyingServiceUrl/vuelos?ids=${ids.joinToString(",")}", Array<VueloResponse>::class.java)
                ?.mapNotNull { vuelo -> vuelo.id?.let { it to vuelo.disponibilidad } }
                ?.toMap()
                .orEmpty()
        } catch (e: RestClientException) {
            null
        }
    }

    /** Sells one seat per flight, all or nothing. Repeating [operacionId] with the same flights sells nothing new. */
    fun vender(operacionId: UUID, vueloIds: List<Long>): ResultadoVentaVuelos =
        try {
            restTemplate.postForEntity("$flyingServiceUrl/vuelos/venta", VentaVuelosRequestDTO(operacionId, vueloIds), Void::class.java)
            ResultadoVentaVuelos.VENDIDA
        } catch (e: HttpClientErrorException) {
            // Only a missing flight or a lack of seats is a rejection; other client errors are integration failures.
            if (e is HttpClientErrorException.NotFound || e is HttpClientErrorException.Conflict) ResultadoVentaVuelos.RECHAZADA
            else ResultadoVentaVuelos.SERVICIO_NO_DISPONIBLE
        } catch (e: RestClientException) {
            ResultadoVentaVuelos.SERVICIO_NO_DISPONIBLE
        }
}

enum class ResultadoVentaVuelos { VENDIDA, RECHAZADA, SERVICIO_NO_DISPONIBLE }

data class VentaVuelosRequestDTO(val operacionId: UUID, val vueloIds: List<Long>)

data class VueloCatalogoDTO(
    val id: Long,
    val origen: String,
    val destino: String,
    val fecha: LocalDate,
    val disponibilidad: Int,
)

data class VueloResponse(
    val id: Long? = null,
    val disponibilidad: Int = 0,
)
