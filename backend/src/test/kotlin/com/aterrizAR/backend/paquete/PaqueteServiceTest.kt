package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
        mockServer.expectDisponibilidades(1L to 10, 2L to 5)

        val created = paqueteService.create(sampleRequest())

        assertEquals("Bariloche", created.destino)
        assertEquals(5, created.disponibilidad)
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
        mockServer.expectDisponibilidades(1L to 10, 2L to 5)
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
        mockServer.expectDisponibilidades(1L to 10, 2L to 5)
        expectVueloCheck(1, 10)
        expectVueloCheck(3, 8)
        mockServer.expectDisponibilidades(1L to 10, 3L to 8)
        val created = paqueteService.create(sampleRequest())

        val updated = paqueteService.update(created.id, sampleRequest(vueloVueltaId = 3))

        assertEquals(3, updated.vueloVueltaId)
        assertEquals(8, updated.disponibilidad)
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
        mockServer.expectDisponibilidades(1L to 10, 2L to 5)
        val created = paqueteService.create(sampleRequest())

        paqueteService.delete(created.id)

        assertEquals(0, paqueteRepository.count())
    }

    @Test
    fun `delete throws 404 when the paquete does not exist`() {
        assertThrows(ResponseStatusException::class.java) { paqueteService.delete(999L) }
    }

    private fun paqueteGuardado(vueloIdaId: Long, vueloVueltaId: Long) = paqueteRepository.save(Paquete(
        precio = 1500f, vueloIdaId = vueloIdaId, vueloVueltaId = vueloVueltaId, hotel = hotel, agencia = agencia,
        origen = "Buenos Aires", destino = "Bariloche",
        fechaInicial = LocalDate.of(2026, 12, 1), fechaFinal = LocalDate.of(2026, 12, 10),
    ))

    @Test
    fun `availability is the lower of both flights using one call for every package`() {
        val primero = paqueteGuardado(1, 2)
        val segundo = paqueteGuardado(3, 4)
        mockServer.expectDisponibilidades(1L to 10, 2L to 7, 3L to 2, 4L to 9)

        val resultado = paqueteService.conDisponibilidad(listOf(primero, segundo))

        assertEquals(listOf(primero.id, segundo.id), resultado.map { it.id })
        assertEquals(listOf(7, 2), resultado.map { it.disponibilidad })
        mockServer.verify()
    }

    @Test
    fun `availability is zero when one of the flights is missing from the answer`() {
        val paquete = paqueteGuardado(1, 2)
        mockServer.expectDisponibilidades(1L to 10, consultados = listOf(1, 2))

        assertEquals(0, paqueteService.conDisponibilidad(listOf(paquete)).single().disponibilidad)
        mockServer.verify()
    }

    @Test
    fun `availability is unknown when flying-service fails`() {
        val paquete = paqueteGuardado(1, 2)
        mockServer.expectDisponibilidadesFallidas(1, 2)

        assertNull(paqueteService.conDisponibilidad(listOf(paquete)).single().disponibilidad)
        mockServer.verify()
    }

    @Test
    fun `no packages means no call to flying-service`() {
        assertEquals(emptyList<PaqueteDTO>(), paqueteService.conDisponibilidad(emptyList()))
        mockServer.verify()
    }

    @Test
    fun `findAll obtener and buscar all carry the availability`() {
        val paquete = paqueteGuardado(1, 2)
        repeat(3) { mockServer.expectDisponibilidades(1L to 4, 2L to 6) }

        assertEquals(4, paqueteService.findAll().single().disponibilidad)
        assertEquals(4, paqueteService.obtener(paquete.id!!).disponibilidad)
        val pagina = paqueteService.buscar(PaqueteBusquedaDTO())
        assertEquals(1, pagina.totalElementos)
        assertEquals(4, pagina.contenido.single().disponibilidad)
        mockServer.verify()
    }

    @Test
    fun `obtener throws 404 without calling flying-service when the paquete does not exist`() {
        assertThrows(ResponseStatusException::class.java) { paqueteService.obtener(999L) }
        mockServer.verify()
    }
}
