package com.backend.flying.service.vuelo

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class VueloService(
    private val vueloRepository: VueloRepository,
) {
    fun findAll(): List<Vuelo> = vueloRepository.findAll()

    fun findById(id: Long): Vuelo =
        vueloRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Vuelo $id no encontrado") }

    fun create(request: VueloRequest): Vuelo {
        validateDisponibilidad(request.capacidad, request.disponibilidad)
        return vueloRepository.save(request.toVuelo())
    }

    fun update(id: Long, request: VueloRequest): Vuelo {
        findById(id)
        validateDisponibilidad(request.capacidad, request.disponibilidad)
        return vueloRepository.save(request.toVuelo(id))
    }

    fun delete(id: Long) {
        findById(id)
        vueloRepository.deleteById(id)
    }

    private fun validateDisponibilidad(capacidad: Int, disponibilidad: Int) {
        if (disponibilidad > capacidad) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "disponibilidad ($disponibilidad) no puede superar la capacidad ($capacidad)",
            )
        }
    }

    private fun VueloRequest.toVuelo(id: Long? = null) = Vuelo(
        id = id,
        aerolinea = aerolinea,
        fecha = fecha,
        hora = hora,
        origen = origen,
        destino = destino,
        capacidad = capacidad,
        disponibilidad = disponibilidad,
    )
}
