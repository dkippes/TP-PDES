package com.aterrizAR.backend.valoracion

import com.aterrizAR.backend.security.UsuarioAutenticado
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Valoraciones", description = "Valoraciones anonimas de paquetes, una por compra")
class ValoracionController(private val valoracionService: ValoracionService) {
    @GetMapping("/api/paquetes/{paqueteId}/valoraciones")
    @Operation(summary = "Lista las valoraciones publicas y el promedio del paquete, sin identificar a sus compradores")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Valoraciones, total y promedio; promedio null si no hay valoraciones"),
        ApiResponse(responseCode = "400", description = "ID no positivo o invalido"),
        ApiResponse(responseCode = "404", description = "Paquete inexistente"),
    ])
    fun listarPublicas(@PathVariable @Positive paqueteId: Long) = valoracionService.listarPublicas(paqueteId)

    @GetMapping("/api/valoraciones/mias")
    @Operation(summary = "Lista las valoraciones propias asociadas a compras")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Valoraciones del comprador autenticado"),
        ApiResponse(responseCode = "401", description = "JWT ausente o invalido"),
        ApiResponse(responseCode = "403", description = "Solo disponible para COMPRADOR"),
        ApiResponse(responseCode = "404", description = "El usuario ya no existe"),
    ])
    fun listarPropias(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt) =
        valoracionService.listarPropias(UsuarioAutenticado.from(jwt))

    @PutMapping("/api/compras/{compraId}/valoracion")
    @Operation(summary = "Crea o edita la valoracion de una compra propia", description = "Puntaje entero de 0 a 10 y comentario opcional de hasta 255 caracteres. Independiente de la compra; reintentos no crean duplicados.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Valoracion guardada"),
        ApiResponse(responseCode = "400", description = "ID, puntaje o comentario invalido"),
        ApiResponse(responseCode = "401", description = "JWT ausente o invalido"),
        ApiResponse(responseCode = "403", description = "Solo disponible para COMPRADOR"),
        ApiResponse(responseCode = "404", description = "Compra inexistente o ajena, o usuario inexistente"),
    ])
    fun guardar(
        @PathVariable @Positive compraId: Long,
        @Valid @RequestBody request: ValoracionRequestDTO,
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
    ) = valoracionService.guardar(compraId, request, UsuarioAutenticado.from(jwt))

    @DeleteMapping("/api/compras/{compraId}/valoracion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina la valoracion de una compra propia", description = "Idempotente: una compra propia sin valoracion tambien devuelve 204. La compra se conserva.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Valoracion eliminada o ya ausente"),
        ApiResponse(responseCode = "400", description = "ID no positivo o invalido"),
        ApiResponse(responseCode = "401", description = "JWT ausente o invalido"),
        ApiResponse(responseCode = "403", description = "Solo disponible para COMPRADOR"),
        ApiResponse(responseCode = "404", description = "Compra inexistente o ajena, o usuario inexistente"),
    ])
    fun eliminar(@PathVariable @Positive compraId: Long, @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt) =
        valoracionService.eliminar(compraId, UsuarioAutenticado.from(jwt))
}
