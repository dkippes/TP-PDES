package com.backend.flying.service.venta

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VentaRepository : JpaRepository<Venta, Long> {
    fun findByOperacionId(operacionId: UUID): Venta?
}
