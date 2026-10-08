package com.aterrizAR.backend.usuario

import com.aterrizAR.backend.security.UsuarioAutenticado
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "ABM de usuarios y cuenta propia")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = [
    ApiResponse(responseCode = "401", description = "JWT ausente o inválido"),
    ApiResponse(responseCode = "403", description = "El ABM es solo para ADMINISTRADOR; /me está disponible para cualquier rol"),
])
class UsuarioController(private val usuarioService: UsuarioService) {
    @GetMapping
    @Operation(summary = "Lista los usuarios, opcionalmente filtrados por rol")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Usuarios encontrados"),
        ApiResponse(responseCode = "400", description = "Rol inválido"),
    ])
    fun findAll(
        @RequestParam(required = false) @Pattern(regexp = "COMPRADOR|AGENTE|ADMINISTRADOR") rol: String?,
    ): List<UsuarioResponseDTO> = usuarioService.findAll(rol).map { it.toDTO() }

    @GetMapping("/me")
    @Operation(summary = "Obtiene el usuario autenticado")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Usuario autenticado"),
        ApiResponse(responseCode = "404", description = "El usuario del token ya no existe"),
    ])
    fun findActual(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): UsuarioResponseDTO =
        usuarioService.findActual(UsuarioAutenticado.from(jwt)).toDTO()

    @PutMapping("/me")
    @Operation(
        summary = "Actualiza los datos personales del usuario autenticado",
        description = "Solo nombre, apellido y dirección; el rol y el correo los gestiona un administrador",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Datos actualizados"),
        ApiResponse(responseCode = "400", description = "Datos inválidos"),
        ApiResponse(responseCode = "404", description = "El usuario del token ya no existe"),
    ])
    fun actualizarDatosPersonales(
        @Valid @RequestBody request: DatosPersonalesRequestDTO,
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
    ): UsuarioResponseDTO = usuarioService.actualizarDatosPersonales(UsuarioAutenticado.from(jwt), request).toDTO()

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Cambia la contraseña del usuario autenticado",
        description = "Requiere la contraseña actual y revoca todas las sesiones renovables del usuario",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Contraseña cambiada"),
        ApiResponse(responseCode = "400", description = "Datos inválidos o contraseña actual incorrecta"),
    ])
    fun cambiarPassword(
        @Valid @RequestBody request: CambioPasswordRequestDTO,
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
    ) = usuarioService.cambiarPassword(UsuarioAutenticado.from(jwt), request)

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un usuario por id")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
    ])
    fun findById(@PathVariable id: Long): UsuarioResponseDTO = usuarioService.findById(id).toDTO()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un usuario con cualquier rol", description = "agenciaId es obligatorio solo para AGENTE")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Usuario creado"),
        ApiResponse(responseCode = "400", description = "Datos inválidos o agenciaId inconsistente con el rol"),
        ApiResponse(responseCode = "404", description = "Agencia no encontrada"),
        ApiResponse(responseCode = "409", description = "Correo duplicado"),
    ])
    fun create(@Valid @RequestBody request: UsuarioCreateRequestDTO): UsuarioResponseDTO =
        usuarioService.create(request).toDTO()

    @PutMapping("/{id}")
    @Operation(
        summary = "Actualiza un usuario",
        description = "Permite cambiar el rol y resetear la contraseña (password opcional). " +
            "Cambiar rol o contraseña revoca las sesiones renovables del usuario.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Usuario actualizado"),
        ApiResponse(responseCode = "400", description = "Datos inválidos o agenciaId inconsistente con el rol"),
        ApiResponse(responseCode = "404", description = "Usuario o agencia no encontrados"),
        ApiResponse(
            responseCode = "409",
            description = "Correo duplicado, el administrador intenta quitarse su propio rol o el perfil anterior tiene compras o valoraciones",
        ),
    ])
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UsuarioUpdateRequestDTO,
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
    ): UsuarioResponseDTO = usuarioService.update(id, request, UsuarioAutenticado.from(jwt)).toDTO()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un usuario, su perfil y sus sesiones")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Usuario eliminado"),
        ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        ApiResponse(responseCode = "409", description = "Es el propio usuario o tiene compras o valoraciones"),
    ])
    fun delete(
        @PathVariable id: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
    ) = usuarioService.delete(id, UsuarioAutenticado.from(jwt))
}
