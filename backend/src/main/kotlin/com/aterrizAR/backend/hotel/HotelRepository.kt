package com.aterrizAR.backend.hotel

import com.aterrizAR.backend.model.Hotel
import org.springframework.data.jpa.repository.JpaRepository

interface HotelRepository : JpaRepository<Hotel, Long> {
    fun findByNombreAndPais(nombre: String, pais: String): Hotel?
}
