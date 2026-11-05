package com.aterrizAR.backend.paquete

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vuelos")
@Tag(name = "Vuelos", description = "Vuelos disponibles para armar paquetes")
@SecurityRequirement(name = "bearerAuth")
class VueloController(private val vueloService: VueloService) {
    @GetMapping
    @Operation(summary = "Lista los vuelos de flying-service, del más próximo al más lejano")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Vuelos con ruta, fecha, hora, aerolínea y lugares disponibles"),
        ApiResponse(responseCode = "401", description = "JWT ausente o inválido"),
        ApiResponse(responseCode = "403", description = "Solo disponible para ADMINISTRADOR o AGENTE"),
        ApiResponse(responseCode = "503", description = "El servicio de vuelos no está disponible"),
    ])
    fun listar(): List<VueloCatalogoDTO> = vueloService.listar()
}
