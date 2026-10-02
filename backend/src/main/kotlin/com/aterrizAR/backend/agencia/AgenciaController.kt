package com.aterrizAR.backend.agencia

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
@RequestMapping("/api/agencias")
@Tag(name = "Agencias", description = "ABM de agencias")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = [
    ApiResponse(responseCode = "401", description = "JWT ausente o inválido"),
    ApiResponse(responseCode = "403", description = "Operación no permitida: crear/eliminar solo ADMINISTRADOR; actualizar ADMINISTRADOR o AGENTE de la agencia"),
])
class AgenciaController(private val agenciaService: AgenciaService) {
    @GetMapping
    @Operation(summary = "Lista todas las agencias")
    fun findAll(): List<AgenciaResponseDTO> = agenciaService.findAll().map { it.toDTO() }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una agencia por id")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Agencia encontrada"),
        ApiResponse(responseCode = "404", description = "Agencia no encontrada"),
    ])
    fun findById(@PathVariable id: Long): AgenciaResponseDTO = agenciaService.findById(id).toDTO()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una agencia")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Agencia creada"),
        ApiResponse(responseCode = "400", description = "Nombre o email inválido"),
        ApiResponse(responseCode = "409", description = "Email duplicado"),
    ])
    fun create(@Valid @RequestBody request: AgenciaRequestDTO): AgenciaResponseDTO =
        agenciaService.create(request).toDTO()

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una agencia", description = "ADMINISTRADOR puede actualizar cualquier agencia; AGENTE solo la agencia asociada a su perfil")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Agencia actualizada"),
        ApiResponse(responseCode = "400", description = "Nombre o email inválido"),
        ApiResponse(responseCode = "404", description = "Agencia no encontrada"),
        ApiResponse(responseCode = "409", description = "Email duplicado"),
    ])
    fun update(@PathVariable id: Long, @Valid @RequestBody request: AgenciaRequestDTO): AgenciaResponseDTO =
        agenciaService.update(id, request).toDTO()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una agencia y sus paquetes en cascada si no hay referencias que lo impidan")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Agencia eliminada"),
        ApiResponse(responseCode = "404", description = "Agencia no encontrada"),
        ApiResponse(responseCode = "409", description = "Referencias existentes impiden eliminar la agencia"),
    ])
    fun delete(@PathVariable id: Long) = agenciaService.delete(id)
}
