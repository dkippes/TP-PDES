package com.aterrizAR.backend.valoracion

import com.aterrizAR.backend.model.Valoracion
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

data class ValoracionRequestDTO(
    // BigDecimal evita que Jackson trunque silenciosamente un puntaje como 1.5 a 1.
    @field:NotNull @field:Min(0) @field:Max(10) @field:Digits(integer = 2, fraction = 0)
    @field:Schema(type = "integer", minimum = "0", maximum = "10")
    val puntaje: BigDecimal?,
    @field:Size(max = 255)
    val comentario: String? = null,
)

/** El contrato publico nunca incluye la identidad del comprador ni de su compra. */
data class ValoracionPublicaDTO(val id: Long, val puntaje: Int, val comentario: String?, val fecha: Instant)

data class ValoracionesPaqueteDTO(
    val promedio: Double?,
    val total: Int,
    val valoraciones: List<ValoracionPublicaDTO>,
)

data class ValoracionPropiaDTO(val compraId: Long, val valoracion: ValoracionPublicaDTO)

fun Valoracion.toDTO() = ValoracionPublicaDTO(requireNotNull(id), puntaje, comentario, fecha)

fun Valoracion.toPropiaDTO() = ValoracionPropiaDTO(requireNotNull(compra?.id), toDTO())
