package com.aterrizAR.backend.hotel

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.server.ResponseStatusException

@SpringBootTest
@ActiveProfiles("test")
class HotelServiceTest {

    @Autowired
    lateinit var hotelService: HotelService

    @Autowired
    lateinit var hotelRepository: HotelRepository

    @AfterEach
    fun cleanUp() {
        hotelRepository.deleteAll()
    }

    private fun sampleRequest(nombre: String = "Sheraton") = HotelRequest(
        nombre = nombre,
        pais = "Argentina",
        foto = "https://example.com/sheraton.jpg",
    )

    @Test
    fun `create persists a hotel and returns it with an id`() {
        val created = hotelService.create(sampleRequest())

        assertEquals("Sheraton", created.nombre)
        assertEquals(1, hotelRepository.count())
    }

    @Test
    fun `findById returns the persisted hotel`() {
        val created = hotelService.create(sampleRequest())

        val fetched = hotelService.findById(created.id!!)

        assertEquals(created.id, fetched.id)
        assertEquals(created.pais, fetched.pais)
    }

    @Test
    fun `findById throws 404 when the hotel does not exist`() {
        assertThrows(ResponseStatusException::class.java) { hotelService.findById(999L) }
    }

    @Test
    fun `update overwrites the existing hotel`() {
        val created = hotelService.create(sampleRequest())

        val updated = hotelService.update(created.id!!, sampleRequest(nombre = "Hilton"))

        assertEquals("Hilton", updated.nombre)
        assertEquals(1, hotelRepository.count())
    }

    @Test
    fun `update throws 404 when the hotel does not exist`() {
        assertThrows(ResponseStatusException::class.java) { hotelService.update(999L, sampleRequest()) }
    }

    @Test
    fun `delete removes the hotel`() {
        val created = hotelService.create(sampleRequest())

        hotelService.delete(created.id!!)

        assertEquals(0, hotelRepository.count())
    }

    @Test
    fun `delete throws 404 when the hotel does not exist`() {
        assertThrows(ResponseStatusException::class.java) { hotelService.delete(999L) }
    }
}
