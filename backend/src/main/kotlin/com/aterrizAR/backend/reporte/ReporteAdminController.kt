package com.aterrizAR.backend.reporte

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/reportes")
@Tag(name = "Reportes Admin", description = "Rankings de compras, ventas y valoraciones")
@SecurityRequirement(name = "bearerAuth")
class ReporteAdminController(private val reporteAdminService: ReporteAdminService) {
    @GetMapping
    @Operation(
        summary = "Obtiene los cuatro Top 5 de administración",
        description = "Cuenta compras registradas y promedia puntajes de valoraciones por destino. " +
            "Agrupa destinos ignorando mayúsculas y espacios en los extremos. " +
            "Desempata por ID ascendente o por destino alfabético.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Cuatro rankings de hasta cinco resultados cada uno"),
        ApiResponse(responseCode = "401", description = "JWT ausente o inválido"),
        ApiResponse(responseCode = "403", description = "Solo disponible para ADMINISTRADOR"),
    ])
    fun obtener(): ReporteAdminDTO = reporteAdminService.obtener()
}
