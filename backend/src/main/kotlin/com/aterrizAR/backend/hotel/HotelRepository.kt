package com.aterrizAR.backend.hotel

import com.aterrizAR.backend.model.Hotel
import org.springframework.data.jpa.repository.JpaRepository

interface HotelRepository : JpaRepository<Hotel, Long>
