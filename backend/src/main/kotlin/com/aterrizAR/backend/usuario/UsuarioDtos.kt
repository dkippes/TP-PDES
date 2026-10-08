package com.aterrizAR.backend.usuario

import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Usuario
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.Locale

private const val ROLES_PATTERN = "COMPRADOR|AGENTE|ADMINISTRADOR"

// La base exige correos en minúsculas (CHECK usuario_correo_normalized); registro, login y ABM normalizan igual.
fun normalizarCorreo(correo: String): String = correo.trim().lowercase(Locale.ROOT)

data class UsuarioCreateRequestDTO(
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
    @field:NotBlank @field:Pattern(regexp = ROLES_PATTERN)
    val rol: String,
    // Obligatorio solo para AGENTE; lo valida el servicio.
    val agenciaId: Long? = null,
)

data class UsuarioUpdateRequestDTO(
    @field:NotBlank @field:Size(max = 255)
    val nombre: String,
    @field:NotBlank @field:Size(max = 255)
    val apellido: String,
    @field:NotBlank @field:Email @field:Size(max = 255)
    val correo: String,
    @field:NotBlank @field:Size(max = 255)
    val direccion: String,
    @field:NotBlank @field:Pattern(regexp = ROLES_PATTERN)
    val rol: String,
    val agenciaId: Long? = null,
    // Opcional: si se envía, el administrador resetea la contraseña.
    @field:Size(min = 8, max = 72)
    val password: String? = null,
)

// Datos que cada usuario puede editar de sí mismo: ni el rol ni el correo (identidad del login).
data class DatosPersonalesRequestDTO(
    @field:NotBlank @field:Size(max = 255)
    val nombre: String,
    @field:NotBlank @field:Size(max = 255)
    val apellido: String,
    @field:NotBlank @field:Size(max = 255)
    val direccion: String,
)

data class CambioPasswordRequestDTO(
    @field:NotBlank @field:Size(max = 72)
    val passwordActual: String,
    @field:NotBlank @field:Size(min = 8, max = 72)
    val passwordNueva: String,
)

data class UsuarioResponseDTO(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val direccion: String,
    val rol: String,
    val agenciaId: Long?,
)

fun Usuario.toDTO(): UsuarioResponseDTO = UsuarioResponseDTO(
    id = requireNotNull(id),
    nombre = nombre,
    apellido = apellido,
    correo = correo,
    direccion = direccion,
    rol = perfil.roleName,
    agenciaId = (perfil as? Agente)?.agencia?.id,
)
