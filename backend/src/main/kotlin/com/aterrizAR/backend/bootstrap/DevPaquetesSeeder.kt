package com.aterrizAR.backend.bootstrap

import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.paquete.PaqueteRepository
import com.aterrizAR.backend.paquete.PaqueteRequest
import com.aterrizAR.backend.paquete.PaqueteService
import com.aterrizAR.backend.paquete.VueloClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import java.time.LocalDate

@Component
@Profile("dev")
class DevPaquetesSeeder(
    private val hotelRepository: HotelRepository,
    private val paqueteRepository: PaqueteRepository,
    private val paqueteService: PaqueteService,
    private val vueloClient: VueloClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun seed(sur: Agencia, andina: Agencia) {
        val vuelos = try {
            vueloClient.findAll()
                .filter { it.disponibilidad > 0 && !it.fecha.isBefore(LocalDate.now()) }
                .sortedBy { it.fecha }
        } catch (exception: RestClientException) {
            logger.warn("No se cargaron paquetes demo: flying-service no está disponible. Se reintentará al reiniciar.")
            return
        }

        val demos = listOf(
            PaqueteDemo("Bariloche", "Hotel del Lago", "Argentina", 230000f, sur),
            PaqueteDemo("Córdoba", "Hotel Córdoba Centro", "Argentina", 180000f, sur),
            PaqueteDemo("Madrid", "Hotel Madrid Centro", "España", 1250000f, andina),
        )
        for (demo in demos) {
            val ida = vuelos.firstOrNull { it.origen == ORIGEN && it.destino == demo.destino } ?: continue
            val vuelta = vuelos.firstOrNull {
                it.origen == demo.destino && it.destino == ORIGEN && !it.fecha.isBefore(ida.fecha.plusDays(2))
            } ?: continue
            val hotel = hotelRepository.findByNombreAndPais(demo.hotel, demo.pais)
                ?: hotelRepository.save(Hotel(nombre = demo.hotel, pais = demo.pais, foto = ""))
            // La identidad demo no depende de las fechas: reiniciar otro día no duplica el paquete.
            if (paqueteRepository.existsByAgencia_IdAndHotel_IdAndDestinoAndOrigen(
                    requireNotNull(demo.agencia.id), requireNotNull(hotel.id), demo.destino, ORIGEN,
                )
            ) continue

            paqueteService.create(
                PaqueteRequest(
                    precio = demo.precio,
                    vueloIdaId = ida.id,
                    vueloVueltaId = vuelta.id,
                    hotelId = requireNotNull(hotel.id),
                    agenciaId = requireNotNull(demo.agencia.id),
                    destino = demo.destino,
                    origen = ORIGEN,
                    fechaInicial = ida.fecha,
                    fechaFinal = vuelta.fecha,
                ),
            )
        }
    }

    private data class PaqueteDemo(
        val destino: String,
        val hotel: String,
        val pais: String,
        val precio: Float,
        val agencia: Agencia,
    )

    private companion object {
        const val ORIGEN = "Buenos Aires"
    }
}
