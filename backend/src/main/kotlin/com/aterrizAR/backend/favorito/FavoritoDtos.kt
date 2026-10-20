package com.aterrizAR.backend.favorito

import com.aterrizAR.backend.model.Favorito
import java.time.Instant

data class FavoritoResponseDTO(val paquete: PaqueteFavoritoDTO, val agregadoEn: Instant)

data class PaqueteFavoritoDTO(val id: Long, val origen: String, val destino: String, val hotel: HotelFavoritoDTO)

data class HotelFavoritoDTO(val nombre: String)

fun Favorito.toDTO() = FavoritoResponseDTO(
    paquete = PaqueteFavoritoDTO(
        id = requireNotNull(paquete.id), origen = paquete.origen, destino = paquete.destino,
        hotel = HotelFavoritoDTO(paquete.hotel.nombre),
    ),
    agregadoEn = agregadoEn,
)
