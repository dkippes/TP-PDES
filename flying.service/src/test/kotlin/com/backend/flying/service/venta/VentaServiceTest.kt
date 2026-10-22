package com.backend.flying.service.venta

import com.backend.flying.service.vuelo.Vuelo
import com.backend.flying.service.vuelo.VueloRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class VentaServiceTest {

    @Autowired
    lateinit var ventaService: VentaService

    @Autowired
    lateinit var ventaRepository: VentaRepository

    @Autowired
    lateinit var vueloRepository: VueloRepository

    @AfterEach
    fun cleanUp() {
        ventaRepository.deleteAll()
        vueloRepository.deleteAll()
    }

    private fun vuelo(disponibilidad: Int) = vueloRepository.save(
        Vuelo(
            aerolinea = "Aerolineas Argentinas",
            fecha = LocalDate.of(2026, 12, 1),
            hora = LocalTime.of(10, 0),
            origen = "EZE",
            destino = "BRC",
            capacidad = 180,
            disponibilidad = disponibilidad,
        ),
    ).id!!

    private fun disponibilidad(id: Long) = vueloRepository.findById(id).get().disponibilidad

    private fun assertStatus(expected: HttpStatus, block: () -> Unit) {
        val error = assertThrows(ResponseStatusException::class.java) { block() }
        assertEquals(expected, error.statusCode)
    }

    @Test
    fun `vender sells one seat on every flight and records the sale`() {
        val ida = vuelo(disponibilidad = 10)
        val vuelta = vuelo(disponibilidad = 1)
        val operacionId = UUID.randomUUID()

        val registrada = ventaService.vender(VentaRequestDTO(operacionId, listOf(vuelta, ida)))

        assertTrue(registrada.nueva)
        assertEquals(listOf(ida, vuelta), registrada.venta.vueloIds)
        assertEquals(9, disponibilidad(ida))
        assertEquals(0, disponibilidad(vuelta))
        assertEquals(1, ventaRepository.count())
    }

    @Test
    fun `vender sells nothing when one flight has no availability`() {
        val ida = vuelo(disponibilidad = 10)
        val vuelta = vuelo(disponibilidad = 0)

        assertStatus(HttpStatus.CONFLICT) {
            ventaService.vender(VentaRequestDTO(UUID.randomUUID(), listOf(ida, vuelta)))
        }

        assertEquals(10, disponibilidad(ida))
        assertEquals(0, disponibilidad(vuelta))
        assertEquals(0, ventaRepository.count())
    }

    @Test
    fun `vender sells nothing when one flight does not exist`() {
        val ida = vuelo(disponibilidad = 10)

        assertStatus(HttpStatus.NOT_FOUND) {
            ventaService.vender(VentaRequestDTO(UUID.randomUUID(), listOf(ida, 999_999L)))
        }

        assertEquals(10, disponibilidad(ida))
        assertEquals(0, ventaRepository.count())
    }

    @Test
    fun `vender returns the original sale when the operacionId is repeated`() {
        val ida = vuelo(disponibilidad = 10)
        val request = VentaRequestDTO(UUID.randomUUID(), listOf(ida))
        val original = ventaService.vender(request)

        val repetida = ventaService.vender(request)

        assertFalse(repetida.nueva)
        assertEquals(original.venta.id, repetida.venta.id)
        assertEquals(9, disponibilidad(ida))
        assertEquals(1, ventaRepository.count())
    }

    @Test
    fun `vender rejects a repeated operacionId with different flights`() {
        val ida = vuelo(disponibilidad = 10)
        val otro = vuelo(disponibilidad = 10)
        val operacionId = UUID.randomUUID()
        ventaService.vender(VentaRequestDTO(operacionId, listOf(ida)))

        assertStatus(HttpStatus.CONFLICT) { ventaService.vender(VentaRequestDTO(operacionId, listOf(otro))) }

        assertEquals(10, disponibilidad(otro))
    }

    @Test
    fun `vender rejects duplicated or non-positive flight ids`() {
        val ida = vuelo(disponibilidad = 10)

        assertStatus(HttpStatus.BAD_REQUEST) { ventaService.vender(VentaRequestDTO(UUID.randomUUID(), listOf(ida, ida))) }
        assertStatus(HttpStatus.BAD_REQUEST) { ventaService.vender(VentaRequestDTO(UUID.randomUUID(), listOf(0L))) }

        assertEquals(10, disponibilidad(ida))
    }
}
