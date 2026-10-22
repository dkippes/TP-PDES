package com.aterrizAR.backend.paquete

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

class VueloClientTest {
    @Test
    fun `flight catalog maps service ids routes dates and availability ignoring unused fields`() {
        val restTemplate = RestTemplate()
        val server = MockRestServiceServer.bindTo(restTemplate).build()
        server.expect(requestTo("http://flying-service:8081/vuelos"))
            .andRespond(withSuccess(
                """[{"id":42,"origen":"Buenos Aires","destino":"Córdoba","fecha":"2026-11-01",
                    "disponibilidad":20,"capacidad":180,"aerolinea":"Demo","hora":"07:30:00"}]""",
                MediaType.APPLICATION_JSON,
            ))

        val client = VueloClient(restTemplate, "http://flying-service:8081")

        assertEquals(listOf(VueloCatalogoDTO(42, "Buenos Aires", "Córdoba", LocalDate.of(2026, 11, 1), 20)), client.findAll())
        server.verify()
    }

    @Test
    fun `availabilities come from one request with distinct ids and omit unknown flights`() {
        val restTemplate = RestTemplate()
        val server = MockRestServiceServer.bindTo(restTemplate).build()
        server.expect(requestTo("http://flying-service:8081/vuelos?ids=1,2,3"))
            .andRespond(withSuccess(
                """[{"id":1,"disponibilidad":10,"capacidad":180},{"id":3,"disponibilidad":0,"capacidad":180}]""",
                MediaType.APPLICATION_JSON,
            ))

        val client = VueloClient(restTemplate, "http://flying-service:8081")

        assertEquals(mapOf(1L to 10, 3L to 0), client.findDisponibilidades(listOf(3, 1, 2, 1)))
        server.verify()
    }

    @Test
    fun `availabilities are null when flying-service fails`() {
        val restTemplate = RestTemplate()
        val server = MockRestServiceServer.bindTo(restTemplate).build()
        server.expect(requestTo("http://flying-service:8081/vuelos?ids=1"))
            .andRespond(withServerError())

        assertNull(VueloClient(restTemplate, "http://flying-service:8081").findDisponibilidades(listOf(1)))
        server.verify()
    }

    @Test
    fun `availabilities of no flights are empty without calling flying-service`() {
        val restTemplate = RestTemplate()
        val server = MockRestServiceServer.bindTo(restTemplate).build()

        assertEquals(emptyMap<Long, Int>(), VueloClient(restTemplate, "http://flying-service:8081").findDisponibilidades(emptyList()))
        server.verify()
    }
}
