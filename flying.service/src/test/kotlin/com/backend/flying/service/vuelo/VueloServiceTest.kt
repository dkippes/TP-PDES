package com.backend.flying.service.vuelo

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.LocalTime

@SpringBootTest
@ActiveProfiles("test")
class VueloServiceTest {

    @Autowired
    lateinit var vueloService: VueloService

    @Autowired
    lateinit var vueloRepository: VueloRepository

    @AfterEach
    fun cleanUp() {
        vueloRepository.deleteAll()
    }

    private fun sampleRequest(disponibilidad: Int = 50) = VueloRequest(
        aerolinea = "Aerolineas Argentinas",
        fecha = LocalDate.of(2026, 5, 1),
        hora = LocalTime.of(14, 30),
        origen = "EZE",
        destino = "COR",
        capacidad = 180,
        disponibilidad = disponibilidad,
    )

    @Test
    fun `create persists a vuelo and returns it with an id`() {
        val created = vueloService.create(sampleRequest())

        assertEquals("EZE", created.origen)
        assertEquals(1, vueloRepository.count())
    }

    @Test
    fun `findById returns the persisted vuelo`() {
        val created = vueloService.create(sampleRequest())

        val fetched = vueloService.findById(created.id!!)

        assertEquals(created.id, fetched.id)
        assertEquals(created.destino, fetched.destino)
    }

    @Test
    fun `findById throws 404 when the vuelo does not exist`() {
        assertThrows(ResponseStatusException::class.java) { vueloService.findById(999L) }
    }

    @Test
    fun `update overwrites the existing vuelo`() {
        val created = vueloService.create(sampleRequest(disponibilidad = 50))

        val updated = vueloService.update(created.id!!, sampleRequest(disponibilidad = 10))

        assertEquals(10, updated.disponibilidad)
        assertEquals(1, vueloRepository.count())
    }

    @Test
    fun `update throws 404 when the vuelo does not exist`() {
        assertThrows(ResponseStatusException::class.java) { vueloService.update(999L, sampleRequest()) }
    }

    @Test
    fun `delete removes the vuelo`() {
        val created = vueloService.create(sampleRequest())

        vueloService.delete(created.id!!)

        assertEquals(0, vueloRepository.count())
    }

    @Test
    fun `delete throws 404 when the vuelo does not exist`() {
        assertThrows(ResponseStatusException::class.java) { vueloService.delete(999L) }
    }

    @Test
    fun `create rejects disponibilidad greater than capacidad`() {
        val invalid = sampleRequest().copy(capacidad = 10, disponibilidad = 50)

        assertThrows(ResponseStatusException::class.java) { vueloService.create(invalid) }
        assertEquals(0, vueloRepository.count())
    }

    @Test
    fun `update rejects disponibilidad greater than capacidad`() {
        val created = vueloService.create(sampleRequest())

        assertThrows(ResponseStatusException::class.java) {
            vueloService.update(created.id!!, sampleRequest().copy(capacidad = 10, disponibilidad = 50))
        }
    }
}
