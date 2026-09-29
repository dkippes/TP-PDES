package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.model.Paquete
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/paquetes")
@Tag(name = "Paquetes", description = "ABM de paquetes turísticos (2 vuelos + 1 hotel)")
class PaqueteController(
    private val paqueteService: PaqueteService,
) {
    @GetMapping
    @Operation(summary = "Lista todos los paquetes")
    fun findAll(): List<Paquete> = paqueteService.findAll()

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un paquete por id")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Paquete encontrado"),
            ApiResponse(responseCode = "404", description = "Paquete no encontrado"),
        ],
    )
    fun findById(@PathVariable id: Long): Paquete = paqueteService.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un paquete, validando hotel, agencia y disponibilidad de ambos vuelos")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Paquete creado"),
            ApiResponse(responseCode = "400", description = "Datos inválidos, hotel/agencia inexistente o vuelo sin disponibilidad"),
            ApiResponse(responseCode = "409", description = "La agencia ya tiene un paquete idéntico"),
        ],
    )
    fun create(@Valid @RequestBody request: PaqueteRequest): Paquete = paqueteService.create(request)

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un paquete existente")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Paquete actualizado"),
            ApiResponse(responseCode = "400", description = "Datos inválidos, hotel/agencia inexistente o vuelo sin disponibilidad"),
            ApiResponse(responseCode = "404", description = "Paquete no encontrado"),
            ApiResponse(responseCode = "409", description = "La agencia ya tiene un paquete idéntico"),
        ],
    )
    fun update(@PathVariable id: Long, @Valid @RequestBody request: PaqueteRequest): Paquete =
        paqueteService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un paquete")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Paquete eliminado"),
            ApiResponse(responseCode = "404", description = "Paquete no encontrado"),
        ],
    )
    fun delete(@PathVariable id: Long) = paqueteService.delete(id)
}
