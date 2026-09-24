package com.backend.flying.service.vuelo

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime

/**
 * Carga vuelos de ejemplo al arrancar con el perfil `dev`, solo si la tabla está vacía.
 * Las fechas son relativas a hoy para que el buscador siempre encuentre vuelos futuros.
 * Para regenerar los datos: `docker compose down -v` y volver a levantar.
 */
@Component
@Profile("dev")
class DevDataSeeder(
    private val vueloRepository: VueloRepository,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        if (vueloRepository.count() > 0) {
            log.info("Seed de vuelos omitido: la tabla ya tiene datos")
            return
        }
        val vuelos = buildVuelos(LocalDate.now())
        vueloRepository.saveAll(vuelos)
        log.info("Seed de vuelos: {} vuelos cargados", vuelos.size)
    }

    private data class Ruta(
        val aerolinea: String,
        val origen: String,
        val destino: String,
        val hora: LocalTime,
        val capacidad: Int,
        val cadaDias: Int,
    )

    private fun buildVuelos(hoy: LocalDate): List<Vuelo> =
        RUTAS.flatMap { ruta ->
            (1..HORIZONTE_DIAS step ruta.cadaDias).map { dia ->
                Vuelo(
                    aerolinea = ruta.aerolinea,
                    fecha = hoy.plusDays(dia.toLong()),
                    hora = ruta.hora,
                    origen = ruta.origen,
                    destino = ruta.destino,
                    capacidad = ruta.capacidad,
                    // Determinístico: varía por ruta y día, siempre entre 0 y capacidad.
                    disponibilidad = (ruta.capacidad * ((dia * 7 + ruta.destino.length) % 10 + 1)) / 10,
                )
            }
        }

    private companion object {
        const val HORIZONTE_DIAS = 30

        val RUTAS = listOf(
            Ruta("Aerolíneas Argentinas", "Buenos Aires", "Bariloche", LocalTime.of(7, 30), 180, 2),
            Ruta("Aerolíneas Argentinas", "Bariloche", "Buenos Aires", LocalTime.of(11, 15), 180, 2),
            Ruta("Aerolíneas Argentinas", "Buenos Aires", "Mendoza", LocalTime.of(9, 0), 150, 3),
            Ruta("Flybondi", "Buenos Aires", "Córdoba", LocalTime.of(6, 45), 186, 2),
            Ruta("Flybondi", "Córdoba", "Buenos Aires", LocalTime.of(20, 10), 186, 2),
            Ruta("JetSMART", "Buenos Aires", "Salta", LocalTime.of(13, 20), 186, 3),
            Ruta("Aerolíneas Argentinas", "Buenos Aires", "Ushuaia", LocalTime.of(5, 50), 189, 4),
            Ruta("Aerolíneas Argentinas", "Buenos Aires", "Madrid", LocalTime.of(22, 30), 300, 5),
            Ruta("Iberia", "Madrid", "Buenos Aires", LocalTime.of(12, 0), 280, 5),
            Ruta("American Airlines", "Buenos Aires", "Miami", LocalTime.of(23, 15), 250, 4),
            Ruta("LATAM", "Buenos Aires", "Río de Janeiro", LocalTime.of(10, 40), 200, 3),
            Ruta("LATAM", "Buenos Aires", "Santiago", LocalTime.of(15, 5), 170, 3),
        )
    }
}
