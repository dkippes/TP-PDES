package com.aterrizAR.backend.paquete

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

/**
 * Consulta puntual de vuelos contra flying-service. Solo necesitamos saber si el vuelo
 * existe y cuánta disponibilidad le queda para poder armar un paquete.
 */
@Component
class VueloClient(
    private val restTemplate: RestTemplate,
    @Value("\${flying.service.url:http://flying-service:8081}") private val flyingServiceUrl: String,
) {
    fun findDisponibilidad(vueloId: Long): Int? =
        try {
            restTemplate.getForObject("$flyingServiceUrl/vuelos/$vueloId", VueloResponse::class.java)?.disponibilidad
        } catch (e: RestClientException) {
            null
        }
}

data class VueloResponse(
    val id: Long? = null,
    val disponibilidad: Int = 0,
)
