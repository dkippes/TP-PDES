package com.aterrizAR.backend.hotel

import com.aterrizAR.backend.model.Hotel
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class HotelService(
    private val hotelRepository: HotelRepository,
) {
    fun findAll(): List<Hotel> = hotelRepository.findAll()

    fun findById(id: Long): Hotel =
        hotelRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel $id no encontrado") }

    fun create(request: HotelRequest): Hotel = hotelRepository.save(request.toHotel())

    fun update(id: Long, request: HotelRequest): Hotel {
        findById(id)
        return hotelRepository.save(request.toHotel(id))
    }

    fun delete(id: Long) {
        findById(id)
        hotelRepository.deleteById(id)
    }

    private fun HotelRequest.toHotel(id: Long? = null) = Hotel(
        id = id,
        nombre = nombre,
        pais = pais,
        foto = foto,
    )
}
