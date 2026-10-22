package com.aterrizAR.backend.favorito

import com.aterrizAR.backend.security.UsuarioAutenticado
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/favoritos")
@Tag(name = "Favoritos", description = "Paquetes favoritos del comprador autenticado")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = [
    ApiResponse(responseCode = "401", description = "JWT ausente o inválido"),
    ApiResponse(responseCode = "403", description = "Solo disponible para COMPRADOR"),
    ApiResponse(responseCode = "404", description = "El usuario del token ya no existe"),
])
class FavoritoController(private val favoritoService: FavoritoService) {
    @GetMapping
    @Operation(summary = "Lista los favoritos propios, del más reciente al más antiguo")
    @ApiResponse(responseCode = "200", description = "Favoritos del comprador, con los datos actuales del paquete y sus lugares disponibles")
    fun listar(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): List<FavoritoResponseDTO> =
        favoritoService.listar(UsuarioAutenticado.from(jwt))

    @PutMapping("/{paqueteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Guarda un paquete como favorito", description = "Repetir la operación no duplica el favorito")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Paquete guardado como favorito"),
        ApiResponse(responseCode = "400", description = "El identificador debe ser positivo"),
        ApiResponse(responseCode = "404", description = "Paquete o usuario inexistente"),
    ])
    fun guardar(
        @PathVariable @Positive paqueteId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
    ) = favoritoService.guardar(paqueteId, UsuarioAutenticado.from(jwt))

    @DeleteMapping("/{paqueteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quita un favorito propio", description = "También responde 204 si ya no estaba guardado o el paquete fue eliminado")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "El paquete ya no está entre los favoritos propios"),
        ApiResponse(responseCode = "400", description = "El identificador debe ser positivo"),
    ])
    fun quitar(
        @PathVariable @Positive paqueteId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
    ) = favoritoService.quitar(paqueteId, UsuarioAutenticado.from(jwt))
}
