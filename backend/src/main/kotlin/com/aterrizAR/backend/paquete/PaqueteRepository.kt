package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.model.Paquete
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface PaqueteRepository : JpaRepository<Paquete, Long>, JpaSpecificationExecutor<Paquete> {
    fun existsByAgencia_IdAndHotel_IdAndDestinoAndOrigen(
        agenciaId: Long,
        hotelId: Long,
        destino: String,
        origen: String,
    ): Boolean

    fun existsByAgencia_IdAndVueloIdaIdAndVueloVueltaIdAndHotel_IdAndIdNot(
        agenciaId: Long,
        vueloIdaId: Long,
        vueloVueltaId: Long,
        hotelId: Long,
        id: Long,
    ): Boolean
}
