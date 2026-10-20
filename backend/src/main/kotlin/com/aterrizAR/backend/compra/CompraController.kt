package com.aterrizAR.backend.compra

import com.aterrizAR.backend.security.UsuarioAutenticado
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/compras")
@Tag(name = "Compras", description = "Compras de paquetes del comprador autenticado")
@SecurityRequirement(name = "bearerAuth")
class CompraController(private val compraService: CompraService) {
    @GetMapping
    @Operation(summary = "Lista las compras propias, de la más reciente a la más antigua")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Compras del comprador, con el precio pagado y los datos actuales del paquete"),
        ApiResponse(responseCode = "401", description = "JWT ausente o inválido"),
        ApiResponse(responseCode = "403", description = "Solo disponible para COMPRADOR"),
        ApiResponse(responseCode = "404", description = "El usuario del token ya no existe"),
    ])
    fun listar(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): List<CompraResponseDTO> =
        compraService.listar(UsuarioAutenticado.from(jwt)).map { it.toDTO() }

    @PostMapping
    @Operation(
        summary = "Compra un paquete",
        description = "Guarda el precio actual del paquete y vende sus vuelos de ida y vuelta. Repetir el mismo operacionId devuelve la compra original.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Compra registrada y vuelos vendidos"),
        ApiResponse(responseCode = "200", description = "La operación ya estaba registrada; se devuelve la compra original"),
        ApiResponse(responseCode = "400", description = "paqueteId u operacionId ausentes o inválidos"),
        ApiResponse(responseCode = "401", description = "JWT ausente o inválido"),
        ApiResponse(responseCode = "403", description = "Solo disponible para COMPRADOR"),
        ApiResponse(responseCode = "404", description = "Paquete o usuario inexistente"),
        ApiResponse(responseCode = "409", description = "Vuelos sin disponibilidad o operacionId usado por otra compra"),
        ApiResponse(responseCode = "503", description = "El servicio de vuelos no está disponible"),
    ])
    fun comprar(
        @Valid @RequestBody request: CompraRequestDTO,
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<CompraResponseDTO> {
        val registrada = compraService.comprar(request, UsuarioAutenticado.from(jwt))
        val status = if (registrada.nueva) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(registrada.compra.toDTO())
    }
}
