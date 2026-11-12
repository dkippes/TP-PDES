package com.aterrizAR.backend.compra

import com.aterrizAR.backend.security.UsuarioAutenticado
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ventas")
@Tag(name = "Ventas", description = "Ventas y clientes de la agencia del agente autenticado")
@SecurityRequirement(name = "bearerAuth")
class VentaController(private val ventaService: VentaService) {
    @GetMapping
    @Operation(summary = "Lista las ventas de mi agencia con los datos del cliente, de la más reciente a la más antigua")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Ventas de la agencia del agente"),
        ApiResponse(responseCode = "401", description = "JWT ausente o inválido"),
        ApiResponse(responseCode = "403", description = "Solo disponible para AGENTE"),
        ApiResponse(responseCode = "404", description = "El usuario del token ya no existe"),
    ])
    fun listar(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): List<VentaResponseDTO> =
        ventaService.listar(UsuarioAutenticado.from(jwt))
}
