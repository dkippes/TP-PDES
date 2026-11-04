package com.aterrizAR.backend.valoracion

import com.aterrizAR.backend.model.Valoracion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ValoracionRepository : JpaRepository<Valoracion, Long> {
    fun findByCompra_Id(compraId: Long): Valoracion?

    fun findAllByPaquete_IdOrderByFechaDescIdDesc(paqueteId: Long): List<Valoracion>

    @Query("SELECT v FROM Valoracion v JOIN FETCH v.compra WHERE v.comprador.id = :compradorId ORDER BY v.fecha DESC, v.id DESC")
    fun findAllPropias(@Param("compradorId") compradorId: Long): List<Valoracion>
}
