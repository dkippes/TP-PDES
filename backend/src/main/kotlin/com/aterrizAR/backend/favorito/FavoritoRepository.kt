package com.aterrizAR.backend.favorito

import com.aterrizAR.backend.model.Favorito
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FavoritoRepository : JpaRepository<Favorito, Long> {
    @Query("""
        SELECT f FROM Favorito f JOIN FETCH f.paquete p JOIN FETCH p.hotel
        WHERE f.comprador.id = :compradorId ORDER BY f.agregadoEn DESC, f.id DESC
    """)
    fun findAllDelComprador(@Param("compradorId") compradorId: Long): List<Favorito>

    fun existsByComprador_IdAndPaquete_Id(compradorId: Long, paqueteId: Long): Boolean

    fun deleteByComprador_IdAndPaquete_Id(compradorId: Long, paqueteId: Long)
}
