package com.aterrizAR.backend.agencia

import com.aterrizAR.backend.model.Agencia
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AgenciaRequestDTO(
    @field:NotBlank @field:Size(max = 255)
    val nombre: String,
    @field:NotBlank @field:Email @field:Size(max = 255)
    val email: String,
) {
    fun toModel(): Agencia = Agencia(nombre = nombre, email = email)
}

data class AgenciaResponseDTO(
    val id: Long,
    val nombre: String,
    val email: String,
)

fun Agencia.toDTO(): AgenciaResponseDTO = AgenciaResponseDTO(
    id = requireNotNull(id),
    nombre = nombre,
    email = email,
)
