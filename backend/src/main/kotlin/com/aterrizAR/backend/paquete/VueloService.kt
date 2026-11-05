package com.aterrizAR.backend.paquete

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.server.ResponseStatusException

@Service
class VueloService(private val vueloClient: VueloClient) {
    /** Flights flying-service offers, soonest first, so agencies can pick the ones of a package. */
    fun listar(): List<VueloCatalogoDTO> =
        try {
            vueloClient.findAll().sortedWith(compareBy({ it.fecha }, { it.hora }, { it.id }))
        } catch (exception: RestClientException) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de vuelos no está disponible", exception)
        }
}
