package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.model.Paquete
import org.springframework.data.jpa.repository.JpaRepository

interface PaqueteRepository : JpaRepository<Paquete, Long> {
    fun existsByAgencia_IdAndVueloIdaIdAndVueloVueltaIdAndHotel_IdAndIdNot(
        agenciaId: Long,
        vueloIdaId: Long,
        vueloVueltaId: Long,
        hotelId: Long,
        id: Long,
    ): Boolean
}
