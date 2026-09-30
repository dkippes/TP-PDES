package com.aterrizAR.backend.hotel

import com.aterrizAR.backend.model.Hotel
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
@RequestMapping("/api/hoteles")
@Tag(name = "Hoteles", description = "ABM de hoteles")
class HotelController(
    private val hotelService: HotelService,
) {
    @GetMapping
    @Operation(summary = "Lista todos los hoteles")
    fun findAll(): List<Hotel> = hotelService.findAll()

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un hotel por id")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Hotel encontrado"),
            ApiResponse(responseCode = "404", description = "Hotel no encontrado"),
        ],
    )
    fun findById(@PathVariable id: Long): Hotel = hotelService.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un hotel")
    fun create(@Valid @RequestBody request: HotelRequest): Hotel = hotelService.create(request)

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un hotel")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Hotel actualizado"),
            ApiResponse(responseCode = "404", description = "Hotel no encontrado"),
        ],
    )
    fun update(@PathVariable id: Long, @Valid @RequestBody request: HotelRequest): Hotel =
        hotelService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un hotel")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Hotel eliminado"),
            ApiResponse(responseCode = "404", description = "Hotel no encontrado"),
        ],
    )
    fun delete(@PathVariable id: Long) = hotelService.delete(id)
}
