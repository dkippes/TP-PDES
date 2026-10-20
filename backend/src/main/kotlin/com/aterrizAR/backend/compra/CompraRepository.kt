package com.aterrizAR.backend.compra

import com.aterrizAR.backend.model.Compra
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CompraRepository : JpaRepository<Compra, Long> {
    fun findByOperacionId(operacionId: UUID): Compra?
}
