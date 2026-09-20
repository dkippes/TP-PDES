package com.backend.flying.service.vuelo

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/vuelos")
@Tag(name = "Vuelos", description = "Alta, consulta, modificación y baja de vuelos")
class VueloController(
    private val vueloService: VueloService,
) {
    @GetMapping
    @Operation(summary = "Lista todos los vuelos")
    @ApiResponse(responseCode = "200", description = "Listado de vuelos")
    fun findAll(): List<Vuelo> = vueloService.findAll()

    @GetMapping("/{id}")
    @Operation(summary = "Busca un vuelo por id")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vuelo encontrado"),
            ApiResponse(responseCode = "404", description = "No existe un vuelo con ese id"),
        ],
    )
    fun findById(@Parameter(description = "Id del vuelo") @PathVariable id: Long): Vuelo =
        vueloService.findById(id)

    @PostMapping
    @Operation(summary = "Crea un vuelo")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Vuelo creado"),
            ApiResponse(responseCode = "400", description = "Datos inválidos (ej. disponibilidad > capacidad)"),
        ],
    )
    fun create(@Valid @RequestBody request: VueloRequest): ResponseEntity<Vuelo> =
        ResponseEntity.status(HttpStatus.CREATED).body(vueloService.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un vuelo existente")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vuelo actualizado"),
            ApiResponse(responseCode = "400", description = "Datos inválidos (ej. disponibilidad > capacidad)"),
            ApiResponse(responseCode = "404", description = "No existe un vuelo con ese id"),
        ],
    )
    fun update(
        @Parameter(description = "Id del vuelo") @PathVariable id: Long,
        @Valid @RequestBody request: VueloRequest,
    ): Vuelo = vueloService.update(id, request)

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un vuelo")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Vuelo eliminado"),
            ApiResponse(responseCode = "404", description = "No existe un vuelo con ese id"),
        ],
    )
    fun delete(@Parameter(description = "Id del vuelo") @PathVariable id: Long): ResponseEntity<Void> {
        vueloService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
