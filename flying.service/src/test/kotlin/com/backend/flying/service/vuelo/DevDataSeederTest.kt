package com.backend.flying.service.vuelo

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalTime

@SpringBootTest
@ActiveProfiles("test")
class DevDataSeederTest {

    @Autowired
    lateinit var vueloRepository: VueloRepository

    @AfterEach
    fun cleanUp() {
        vueloRepository.deleteAll()
    }

    @Test
    fun `empty database receives future flights with valid availability`() {
        val before = LocalDate.now()

        DevDataSeeder(vueloRepository).run()

        val after = LocalDate.now()
        val vuelos = vueloRepository.findAll()
        assertEquals(128, vuelos.size)
        assertEquals(12, vuelos.map { it.origen to it.destino }.distinct().size)
        assertTrue(vuelos.all { it.fecha > before && it.fecha <= after.plusDays(30) })
        assertTrue(vuelos.all { it.capacidad > 0 && it.disponibilidad in 0..it.capacidad })
        assertTrue(vuelos.all { it.aerolinea.isNotBlank() && it.origen != it.destino })
        assertEquals(15, vuelos.count { it.origen == "Buenos Aires" && it.destino == "Bariloche" })
    }

    @Test
    fun `existing flights are preserved and repeated startup does not add demo flights`() {
        val existing = vueloRepository.save(
            Vuelo(
                aerolinea = "Test Airline",
                fecha = LocalDate.now().plusDays(2),
                hora = LocalTime.of(12, 0),
                origen = "EZE",
                destino = "COR",
                capacidad = 100,
                disponibilidad = 42,
            ),
        )
        val seeder = DevDataSeeder(vueloRepository)

        seeder.run()
        seeder.run()

        val vuelos = vueloRepository.findAll()
        assertEquals(1, vuelos.size)
        assertEquals(existing.id, vuelos.single().id)
        assertEquals(42, vuelos.single().disponibilidad)
        assertEquals("Test Airline", vuelos.single().aerolinea)
    }
}
