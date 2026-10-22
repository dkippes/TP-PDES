package com.backend.flying.service.vuelo

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class VueloService(
    private val vueloRepository: VueloRepository,
) {
    /** Without [ids] returns every flight; with [ids] only the existing ones (missing ids are absent, empty list gives empty). */
    fun findAll(ids: List<Long>? = null): List<Vuelo> =
        if (ids == null) vueloRepository.findAll() else vueloRepository.findAllById(ids.distinct())

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
