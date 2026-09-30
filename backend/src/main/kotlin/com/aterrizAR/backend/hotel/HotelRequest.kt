package com.aterrizAR.backend.hotel

import jakarta.validation.constraints.NotBlank

data class HotelRequest(
    @field:NotBlank
    val nombre: String,

    @field:NotBlank
    val pais: String,

    @field:NotBlank
    val foto: String,
)
