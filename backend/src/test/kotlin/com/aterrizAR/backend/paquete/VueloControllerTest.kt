package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.security.authenticatedAs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VueloControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var restTemplate: RestTemplate
    @Autowired lateinit var jwtService: JwtService

    private lateinit var mockServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build()
    }

    private fun expectVuelos() = mockServer.expect(requestTo("http://flying-service:8081/vuelos"))

    @Test
    fun `lists flights soonest first with route airline and seats`() {
        expectVuelos().andRespond(withSuccess(
            """[{"id":2,"origen":"Córdoba","destino":"Buenos Aires","fecha":"2027-01-10","hora":"10:00:00","aerolinea":"B","disponibilidad":3,"capacidad":10},
                {"id":1,"origen":"Buenos Aires","destino":"Córdoba","fecha":"2027-01-05","hora":"08:30:00","aerolinea":"A","disponibilidad":7,"capacidad":10}]""",
            MediaType.APPLICATION_JSON,
        ))

        mockMvc.perform(get("/api/vuelos").authenticatedAs(jwtService))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].origen").value("Buenos Aires"))
            .andExpect(jsonPath("$[0].aerolinea").value("A"))
            .andExpect(jsonPath("$[0].hora").value("08:30:00"))
            .andExpect(jsonPath("$[0].disponibilidad").value(7))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[0].capacidad").doesNotExist())
    }

    @Test
    fun `an unavailable flying service answers 503`() {
        expectVuelos().andRespond(withServerError())

        mockMvc.perform(get("/api/vuelos").authenticatedAs(jwtService)).andExpect(status().isServiceUnavailable)
    }

    @Test
    fun `buyers and visitors cannot list flights`() {
        mockMvc.perform(get("/api/vuelos")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/vuelos").authenticatedAs(jwtService, Comprador())).andExpect(status().isForbidden)
    }
}
