package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Hotel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
class PaqueteServiceTest {

    @Autowired
    lateinit var paqueteService: PaqueteService

    @Autowired
    lateinit var paqueteRepository: PaqueteRepository

    @Autowired
    lateinit var hotelRepository: HotelRepository

    @Autowired
    lateinit var agenciaRepository: AgenciaRepository

    @Autowired
    lateinit var restTemplate: RestTemplate

    private lateinit var mockServer: MockRestServiceServer
    private lateinit var hotel: Hotel
    private lateinit var agencia: Agencia

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build()
        hotel = hotelRepository.save(Hotel(nombre = "Sheraton", pais = "Argentina", foto = "sheraton.jpg"))
        agencia = agenciaRepository.save(Agencia(nombre = "Agencia Sur", email = "agencia-sur@test.com"))
    }

    @AfterEach
    fun cleanUp() {
        paqueteRepository.deleteAll()
        hotelRepository.deleteAll()
        agenciaRepository.deleteAll()
    }

    private fun sampleRequest(vueloIdaId: Long = 1, vueloVueltaId: Long = 2) = PaqueteRequest(
        precio = 1500f,
        vueloIdaId = vueloIdaId,
        vueloVueltaId = vueloVueltaId,
        hotelId = hotel.id!!,
        agenciaId = agencia.id!!,
        destino = "Bariloche",
        origen = "Buenos Aires",
        fechaInicial = LocalDate.of(2026, 12, 1),
        fechaFinal = LocalDate.of(2026, 12, 10),
    )

    private fun expectVueloCheck(vueloId: Long, disponibilidad: Int) {
        mockServer.expect(requestTo("http://flying-service:8081/vuelos/$vueloId"))
            .andRespond(
                withSuccess(
                    """{"id":$vueloId,"disponibilidad":$disponibilidad}""",
                    MediaType.APPLICATION_JSON,
                ),
            )
    }

    @Test
    fun `create persists a paquete when both flights have availability`() {
        expectVueloCheck(1, 10)
        expectVueloCheck(2, 5)

        val created = paqueteService.create(sampleRequest())

        assertEquals("Bariloche", created.destino)
        assertEquals(1, paqueteRepository.count())
    }

    @Test
    fun `create throws 400 when the hotel does not exist`() {
        assertThrows(ResponseStatusException::class.java) {
            paqueteService.create(sampleRequest().copy(hotelId = 999L))
        }
        assertEquals(0, paqueteRepository.count())
    }

    @Test
    fun `create throws 400 when the agencia does not exist`() {
        assertThrows(ResponseStatusException::class.java) {
            paqueteService.create(sampleRequest().copy(agenciaId = 999L))
        }
        assertEquals(0, paqueteRepository.count())
    }

    @Test
    fun `create throws 400 when fechaFinal is before fechaInicial`() {
        val invalid = sampleRequest().copy(
            fechaInicial = LocalDate.of(2026, 12, 10),
            fechaFinal = LocalDate.of(2026, 12, 1),
        )

        assertThrows(ResponseStatusException::class.java) { paqueteService.create(invalid) }
        assertEquals(0, paqueteRepository.count())
    }

    @Test
    fun `create throws 400 when a flight does not exist`() {
        mockServer.expect(requestTo("http://flying-service:8081/vuelos/1"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound())

        assertThrows(ResponseStatusException::class.java) { paqueteService.create(sampleRequest()) }
        assertEquals(0, paqueteRepository.count())
    }

    @Test
    fun `create throws 400 when a flight has no availability`() {
        expectVueloCheck(1, 0)

        assertThrows(ResponseStatusException::class.java) { paqueteService.create(sampleRequest()) }
        assertEquals(0, paqueteRepository.count())
    }

    @Test
    fun `create throws 409 when the agencia already has an identical paquete`() {
        expectVueloCheck(1, 10)
        expectVueloCheck(2, 5)
        expectVueloCheck(1, 10)
        expectVueloCheck(2, 5)
        paqueteService.create(sampleRequest())

        val exception = assertThrows(ResponseStatusException::class.java) {
            paqueteService.create(sampleRequest())
        }
        assertEquals(409, exception.statusCode.value())
        assertEquals(1, paqueteRepository.count())
    }

    @Test
    fun `findById throws 404 when the paquete does not exist`() {
        assertThrows(ResponseStatusException::class.java) { paqueteService.findById(999L) }
    }

    @Test
    fun `update overwrites the existing paquete`() {
        expectVueloCheck(1, 10)
        expectVueloCheck(2, 5)
        expectVueloCheck(1, 10)
        expectVueloCheck(3, 8)
        val created = paqueteService.create(sampleRequest())

        val updated = paqueteService.update(created.id!!, sampleRequest(vueloVueltaId = 3))

        assertEquals(3, updated.vueloVueltaId)
        assertEquals(1, paqueteRepository.count())
    }

    @Test
    fun `update throws 404 when the paquete does not exist`() {
        assertThrows(ResponseStatusException::class.java) { paqueteService.update(999L, sampleRequest()) }
    }

    @Test
    fun `delete removes the paquete`() {
        expectVueloCheck(1, 10)
        expectVueloCheck(2, 5)
        val created = paqueteService.create(sampleRequest())

        paqueteService.delete(created.id!!)

        assertEquals(0, paqueteRepository.count())
    }

    @Test
    fun `delete throws 404 when the paquete does not exist`() {
        assertThrows(ResponseStatusException::class.java) { paqueteService.delete(999L) }
    }
}
