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

    // El comprador es un Perfil; sus datos de contacto están en el Usuario que lo posee.
    @Query("""
        SELECT new com.aterrizAR.backend.compra.VentaResponseDTO(
            c.id, c.operacionId, c.precio, c.fecha, p.id, p.origen, p.destino, h.nombre,
            new com.aterrizAR.backend.compra.ClienteDTO(u.id, u.nombre, u.apellido, u.correo)
        )
        FROM Compra c JOIN c.paquete p JOIN p.hotel h, Usuario u
        WHERE c.agencia.id = :agenciaId AND u.perfil.id = c.comprador.id
        ORDER BY c.fecha DESC, c.id DESC
    """)
    fun findVentasDeLaAgencia(@Param("agenciaId") agenciaId: Long): List<VentaResponseDTO>
}
