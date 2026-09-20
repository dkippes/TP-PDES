package com.backend.flying.service.vuelo

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDate
import java.time.LocalTime

data class VueloRequest(
    @field:NotBlank
    val aerolinea: String,

    val fecha: LocalDate,

    val hora: LocalTime,

    @field:NotBlank
    val origen: String,

    @field:NotBlank
    val destino: String,

    @field:Positive
    val capacidad: Int,

    @field:PositiveOrZero
    val disponibilidad: Int,
)
