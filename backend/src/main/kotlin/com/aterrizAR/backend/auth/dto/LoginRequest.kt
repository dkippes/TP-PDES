package com.aterrizAR.backend.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank @field:Email val correo: String,
    @field:NotBlank val password: String,
)
