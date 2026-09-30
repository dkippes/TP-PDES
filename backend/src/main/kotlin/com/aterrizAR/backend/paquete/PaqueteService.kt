package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class PaqueteService(
    private val paqueteRepository: PaqueteRepository,
    private val hotelRepository: HotelRepository,
    private val agenciaRepository: AgenciaRepository,
    private val vueloClient: VueloClient,
) {
    fun findAll(): List<Paquete> = paqueteRepository.findAll()

    fun findById(id: Long): Paquete =
        paqueteRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Paquete $id no encontrado") }

    fun create(request: PaqueteRequest): Paquete = paqueteRepository.save(validate(request, excludeId = null))

    fun update(id: Long, request: PaqueteRequest): Paquete {
        findById(id)
        return paqueteRepository.save(validate(request, excludeId = id))
    }

    fun delete(id: Long) {
        findById(id)
        paqueteRepository.deleteById(id)
    }

    private fun validate(request: PaqueteRequest, excludeId: Long?): Paquete {
        val hotel = findHotel(request.hotelId)
        val agencia = findAgencia(request.agenciaId)
        validateFechas(request)
        validateVuelo(request.vueloIdaId)
        validateVuelo(request.vueloVueltaId)
        validateUnicoPorAgencia(request, excludeId)
        return request.toPaquete(hotel, agencia, excludeId)
    }

    private fun findHotel(id: Long): Hotel =
        hotelRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Hotel $id no encontrado") }

    private fun findAgencia(id: Long): Agencia =
        agenciaRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Agencia $id no encontrada") }

    private fun validateFechas(request: PaqueteRequest) {
        if (request.fechaFinal.isBefore(request.fechaInicial)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "fechaFinal (${request.fechaFinal}) no puede ser anterior a fechaInicial (${request.fechaInicial})",
            )
        }
    }

    private fun validateVuelo(vueloId: Long) {
        val disponibilidad = vueloClient.findDisponibilidad(vueloId)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Vuelo $vueloId no existe")
        if (disponibilidad <= 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Vuelo $vueloId sin disponibilidad")
        }
    }

    private fun validateUnicoPorAgencia(request: PaqueteRequest, excludeId: Long?) {
        val duplicado = paqueteRepository.existsByAgencia_IdAndVueloIdaIdAndVueloVueltaIdAndHotel_IdAndIdNot(
            request.agenciaId,
            request.vueloIdaId,
            request.vueloVueltaId,
            request.hotelId,
            excludeId ?: -1L,
        )
        if (duplicado) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "La agencia ${request.agenciaId} ya tiene un paquete con esos vuelos y ese hotel",
            )
        }
    }

    private fun PaqueteRequest.toPaquete(hotel: Hotel, agencia: Agencia, id: Long? = null) = Paquete(
        id = id,
        precio = precio,
        vueloIdaId = vueloIdaId,
        vueloVueltaId = vueloVueltaId,
        hotel = hotel,
        agencia = agencia,
        destino = destino,
        origen = origen,
        fechaInicial = fechaInicial,
        fechaFinal = fechaFinal,
    )
}
