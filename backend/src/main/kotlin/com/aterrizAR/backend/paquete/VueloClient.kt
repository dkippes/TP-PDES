package com.aterrizAR.backend.paquete

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

/**
 * Acceso al catálogo de flying-service para validar vuelos y vincular los datos demo.
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
}

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
