package com.backend.flying.service.vuelo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface VueloRepository : JpaRepository<Vuelo, Long> {
    /** Checks and decrements availability in one statement; returns 0 when the flight is missing or full. */
    @Modifying
    @Query("UPDATE Vuelo v SET v.disponibilidad = v.disponibilidad - 1 WHERE v.id = :id AND v.disponibilidad > 0")
    fun descontarAsiento(id: Long): Int
}
