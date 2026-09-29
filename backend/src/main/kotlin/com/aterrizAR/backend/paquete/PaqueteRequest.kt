package com.aterrizAR.backend.paquete

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.LocalDate

data class PaqueteRequest(
    @field:Positive
    val precio: Float,

    @field:Positive
    val vueloIdaId: Long,

    @field:Positive
    val vueloVueltaId: Long,

    @field:Positive
    val hotelId: Long,

    @field:Positive
    val agenciaId: Long,

    @field:NotBlank
    val destino: String,

    @field:NotBlank
    val origen: String,

    val fechaInicial: LocalDate,

    val fechaFinal: LocalDate,
)
