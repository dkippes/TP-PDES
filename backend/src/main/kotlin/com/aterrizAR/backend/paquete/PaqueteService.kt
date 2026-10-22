package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
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
    fun findAll(): List<PaqueteDTO> = conDisponibilidad(paqueteRepository.findAll())

    fun buscar(filtros: PaqueteBusquedaDTO): PaquetesPaginaDTO {
        val pagina = consultar(filtros)
        return pagina.toDTO(conDisponibilidad(pagina.content))
    }

    /** Applies the search filters, order and pagination over the stored packages, without availability. */
    fun consultar(filtros: PaqueteBusquedaDTO): Page<Paquete> {
        val normalizados = filtros.copy(
            q = filtros.q.normalizado(),
            destino = filtros.destino.normalizado(),
            origen = filtros.origen.normalizado(),
        )
        validateRango(normalizados.precioMin, normalizados.precioMax, "precioMin", "precioMax")
        validateRango(normalizados.fechaDesde, normalizados.fechaHasta, "fechaDesde", "fechaHasta")
        val pageable = PageRequest.of(normalizados.pagina, normalizados.tamanio, normalizados.orden.sort)
        return paqueteRepository.findAll(PaqueteSpecifications.from(normalizados), pageable)
    }

    private fun String?.normalizado(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun <T : Comparable<T>> validateRango(desde: T?, hasta: T?, campoDesde: String, campoHasta: String) {
        if (desde != null && hasta != null && desde > hasta) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "$campoDesde ($desde) no puede ser mayor que $campoHasta ($hasta)",
            )
        }
    }

    fun findById(id: Long): Paquete =
        paqueteRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Paquete $id no encontrado") }

    fun obtener(id: Long): PaqueteDTO = conDisponibilidad(listOf(findById(id))).single()

    fun create(request: PaqueteRequest): PaqueteDTO =
        conDisponibilidad(listOf(paqueteRepository.save(validate(request, excludeId = null)))).single()

    fun update(id: Long, request: PaqueteRequest): PaqueteDTO {
        findById(id)
        return conDisponibilidad(listOf(paqueteRepository.save(validate(request, excludeId = id)))).single()
    }

    /**
     * The packages as catalog DTOs, in the same order, with one flying-service call for the whole list.
     * Seats left are the lower availability of the outbound and return flights: null when flying-service did not
     * answer (unknown) and 0 when one of the package's flights is missing from its answer.
     */
    fun conDisponibilidad(paquetes: List<Paquete>): List<PaqueteDTO> {
        val porVuelo = vueloClient.findDisponibilidades(paquetes.flatMap { listOf(it.vueloIdaId, it.vueloVueltaId) })
        return paquetes.map { paquete ->
            paquete.toDTO(porVuelo?.let { minOf(it[paquete.vueloIdaId] ?: 0, it[paquete.vueloVueltaId] ?: 0) })
        }
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
