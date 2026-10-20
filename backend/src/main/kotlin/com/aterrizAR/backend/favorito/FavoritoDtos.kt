package com.aterrizAR.backend.favorito

import com.aterrizAR.backend.model.Favorito
import com.aterrizAR.backend.model.Paquete
import java.time.Instant

/** `paquete` keeps the same shape as the catalog (`GET /api/paquetes`) so clients render both with one model. */
data class FavoritoResponseDTO(val paquete: Paquete, val agregadoEn: Instant)

fun Favorito.toDTO() = FavoritoResponseDTO(paquete = paquete, agregadoEn = agregadoEn)
