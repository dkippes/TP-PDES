package com.aterrizAR.backend.favorito

import com.aterrizAR.backend.model.Favorito
import com.aterrizAR.backend.paquete.PaqueteDTO
import java.time.Instant

/** `paquete` keeps the same shape as the catalog (`GET /api/paquetes`) so clients render both with one model. */
data class FavoritoResponseDTO(val paquete: PaqueteDTO, val agregadoEn: Instant)

fun Favorito.toDTO(paquete: PaqueteDTO) = FavoritoResponseDTO(paquete = paquete, agregadoEn = agregadoEn)
