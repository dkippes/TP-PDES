package com.aterrizAR.backend.compra

import java.time.LocalDate
import java.util.UUID

/** Una venta de la agencia: lo que pagó el cliente, el paquete vendido y los datos de contacto del comprador. */
data class VentaResponseDTO(
    val id: Long,
    val operacionId: UUID,
    val precio: Float,
    val fecha: LocalDate,
    val paqueteId: Long,
    val origen: String,
    val destino: String,
    val hotel: String,
    val cliente: ClienteDTO,
)

data class ClienteDTO(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val correo: String,
)
