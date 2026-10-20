package com.backend.flying.service.venta

import jakarta.validation.constraints.NotEmpty
import java.time.Instant
import java.util.UUID

data class VentaRequestDTO(
    val operacionId: UUID,

    @field:NotEmpty
    val vueloIds: List<Long>,
)

data class VentaDTO(
    val operacionId: UUID,
    val vueloIds: List<Long>,
    val creadaEn: Instant,
)
