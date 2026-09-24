package com.aterrizAR.backend.auth

import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.Locale

data class RegisterRequestDTO(
    @field:NotBlank @field:Size(max = 255)
    val nombre: String,
    @field:NotBlank @field:Size(max = 255)
    val apellido: String,
    @field:NotBlank @field:Email @field:Size(max = 255)
    val correo: String,
    @field:NotBlank @field:Size(max = 255)
    val direccion: String,
    @field:NotBlank @field:Size(min = 8, max = 72)
    val password: String,
) {
    fun toModel(passwordHash: String, perfil: Perfil): Usuario = Usuario(
        nombre = nombre.trim(),
        apellido = apellido.trim(),
        correo = correo.trim().lowercase(Locale.ROOT),
        direccion = direccion.trim(),
        passwordHash = passwordHash,
        perfil = perfil,
    )
}

data class LoginRequestDTO(
    @field:NotBlank @field:Email @field:Size(max = 255)
    val correo: String,
    @field:NotBlank @field:Size(min = 8, max = 72)
    val password: String,
)

data class PublicUserResponseDTO(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val direccion: String,
    val role: String,
)

fun Usuario.toDTO(): PublicUserResponseDTO = PublicUserResponseDTO(
    id = requireNotNull(id),
    nombre = nombre,
    apellido = apellido,
    correo = correo,
    direccion = direccion,
    role = perfil.roleName,
)

data class LoginResponseDTO(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: PublicUserResponseDTO,
)

data class ApiErrorResponseDTO(
    val status: Int,
    val error: String,
    val message: String,
    val fieldErrors: Map<String, String>? = null,
)
