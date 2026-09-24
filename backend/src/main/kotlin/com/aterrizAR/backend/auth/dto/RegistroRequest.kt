package com.aterrizAR.backend.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegistroRequest(
    @field:NotBlank @field:Size(max = 255) val nombre: String,
    @field:NotBlank @field:Size(max = 255) val apellido: String,
    @field:NotBlank @field:Email @field:Size(max = 255) val correo: String,
    @field:NotBlank @field:Size(max = 255) val direccion: String,
    @field:NotBlank @field:Size(min = 4, max = 72) val password: String,
)
