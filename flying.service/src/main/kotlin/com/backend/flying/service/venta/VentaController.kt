package com.backend.flying.service.venta

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/vuelos/venta")
@Tag(name = "Ventas", description = "Venta de asientos")
class VentaController(
    private val ventaService: VentaService,
) {
    @PostMapping
    @Operation(
        summary = "Vende un asiento en cada vuelo indicado",
        description = "Todos los vuelos se venden o ninguno. Repetir el operacionId con los mismos vuelos " +
                "devuelve la venta original sin volver a descontar asientos.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Venta registrada"),
            ApiResponse(responseCode = "200", description = "La operación ya estaba registrada"),
            ApiResponse(responseCode = "400", description = "Lista vacía, ids repetidos o no positivos"),
            ApiResponse(responseCode = "404", description = "Algún vuelo no existe"),
            ApiResponse(
                responseCode = "409",
                description = "Algún vuelo sin disponibilidad, o operacionId usado con otros vuelos",
            ),
        ],
    )
    fun vender(@Valid @RequestBody request: VentaRequestDTO): ResponseEntity<VentaDTO> {
        val registrada = ventaService.vender(request)
        val status = if (registrada.nueva) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(registrada.venta.toDTO())
    }
}
