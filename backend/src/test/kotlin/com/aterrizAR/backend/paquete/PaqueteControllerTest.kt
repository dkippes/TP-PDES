package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.security.authenticatedAs
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaqueteControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var paqueteRepository: PaqueteRepository

    @Autowired
    lateinit var hotelRepository: HotelRepository

    @Autowired
    lateinit var agenciaRepository: AgenciaRepository

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Autowired
    lateinit var jwtService: JwtService

    private fun perform(request: MockHttpServletRequestBuilder) =
        mockMvc.perform(request.authenticatedAs(jwtService))

    private lateinit var mockServer: MockRestServiceServer
    private lateinit var hotel: Hotel
    private lateinit var agencia: Agencia

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build()
        hotel = hotelRepository.save(Hotel(nombre = "Sheraton", pais = "Argentina", foto = "sheraton.jpg"))
        agencia = agenciaRepository.save(Agencia(nombre = "Agencia Sur", email = "agencia-sur-ctrl@test.com"))
    }

    @AfterEach
    fun cleanUp() {
        paqueteRepository.deleteAll()
        hotelRepository.deleteAll()
        agenciaRepository.deleteAll()
    }

    private fun expectVueloCheck(vueloId: Long, disponibilidad: Int) {
        mockServer.expect(requestTo("http://flying-service:8081/vuelos/$vueloId"))
            .andRespond(
                withSuccess(
                    """{"id":$vueloId,"disponibilidad":$disponibilidad}""",
                    MediaType.APPLICATION_JSON,
                ),
            )
    }

    private fun paqueteJson(vueloIdaId: Long = 1, vueloVueltaId: Long = 2) = """
        {"precio":1500.0,"vueloIdaId":$vueloIdaId,"vueloVueltaId":$vueloVueltaId,
         "hotelId":${hotel.id},"agenciaId":${agencia.id},"destino":"Bariloche","origen":"Buenos Aires",
         "fechaInicial":"2026-12-01","fechaFinal":"2026-12-10"}
    """.trimIndent()

    @Test
    fun `POST paquetes creates a paquete`() {
        expectVueloCheck(1, 10)
        expectVueloCheck(2, 5)

        perform(post("/api/paquetes").contentType(MediaType.APPLICATION_JSON).content(paqueteJson()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.destino").value("Bariloche"))
    }

    @Test
    fun `POST paquetes rejects a flight without availability`() {
        expectVueloCheck(1, 0)

        perform(post("/api/paquetes").contentType(MediaType.APPLICATION_JSON).content(paqueteJson()))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST paquetes rejects blank required fields`() {
        val invalidJson = """
            {"precio":1500.0,"vueloIdaId":1,"vueloVueltaId":2,
             "hotelId":${hotel.id},"agenciaId":${agencia.id},"destino":"","origen":"Buenos Aires",
             "fechaInicial":"2026-12-01","fechaFinal":"2026-12-10"}
        """.trimIndent()

        perform(post("/api/paquetes").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET paquetes id returns 404 when missing`() {
        perform(get("/api/paquetes/999")).andExpect(status().isNotFound)
    }

    @Test
    fun `full crud flow through the HTTP layer`() {
        expectVueloCheck(1, 10)
        expectVueloCheck(2, 5)

        val createResult = perform(
            post("/api/paquetes").contentType(MediaType.APPLICATION_JSON).content(paqueteJson()),
        ).andExpect(status().isCreated).andReturn()

        val id = "\"id\":(\\d+)".toRegex()
            .find(createResult.response.contentAsString)!!.groupValues[1]

        perform(get("/api/paquetes/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.destino").value("Bariloche"))

        perform(get("/api/paquetes"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }
}
