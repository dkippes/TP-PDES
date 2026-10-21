package com.aterrizAR.backend.compra

import com.aterrizAR.backend.model.Compra
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface CompraRepository : JpaRepository<Compra, Long> {
    fun findByOperacionId(operacionId: UUID): Compra?

    @Query("""
        SELECT c FROM Compra c JOIN FETCH c.paquete p JOIN FETCH p.hotel JOIN FETCH p.agencia
        WHERE c.comprador.id = :compradorId ORDER BY c.fecha DESC, c.id DESC
    """)
    fun findAllDelComprador(@Param("compradorId") compradorId: Long): List<Compra>
}
