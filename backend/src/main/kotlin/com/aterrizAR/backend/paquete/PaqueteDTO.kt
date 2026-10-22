package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/** Catalog shape of a package: the entity's fields plus the seats left. */
data class PaqueteDTO(
    val id: Long,
    val precio: Float,
    val vueloIdaId: Long,
    val vueloVueltaId: Long,
    val hotel: Hotel,
    val agencia: Agencia,
    val destino: String,
    val origen: String,
    val fechaInicial: LocalDate,
    val fechaFinal: LocalDate,
    @field:Schema(
        description = "Lugares disponibles: el menor entre los vuelos de ida y vuelta (0 si falta alguno). " +
            "Es null cuando flying-service no respondió",
        nullable = true,
    )
    val disponibilidad: Int?,
)

fun Paquete.toDTO(disponibilidad: Int?) = PaqueteDTO(
    id = requireNotNull(id),
    precio = precio,
    vueloIdaId = vueloIdaId,
    vueloVueltaId = vueloVueltaId,
    hotel = hotel,
    agencia = agencia,
    destino = destino,
    origen = origen,
    fechaInicial = fechaInicial,
    fechaFinal = fechaFinal,
    disponibilidad = disponibilidad,
)
