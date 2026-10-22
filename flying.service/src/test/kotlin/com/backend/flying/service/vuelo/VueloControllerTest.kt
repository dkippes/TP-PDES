package com.backend.flying.service.vuelo

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VueloControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var vueloRepository: VueloRepository

    @AfterEach
    fun cleanUp() {
        vueloRepository.deleteAll()
    }

    private fun vueloJson(disponibilidad: Int = 50, capacidad: Int = 180) = """
        {"aerolinea":"Aerolineas Argentinas","fecha":"2026-05-01","hora":"14:30:00",
         "origen":"EZE","destino":"COR","capacidad":$capacidad,"disponibilidad":$disponibilidad}
    """.trimIndent()

    @Test
    fun `POST vuelos creates a vuelo`() {
        mockMvc.perform(post("/vuelos").contentType(MediaType.APPLICATION_JSON).content(vueloJson()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.origen").value("EZE"))
    }

    @Test
    fun `POST vuelos rejects disponibilidad greater than capacidad`() {
        mockMvc.perform(
            post("/vuelos").contentType(MediaType.APPLICATION_JSON)
                .content(vueloJson(disponibilidad = 50, capacidad = 10)),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST vuelos rejects blank required fields`() {
        val invalidJson = """
            {"aerolinea":"","fecha":"2026-05-01","hora":"14:30:00",
             "origen":"EZE","destino":"COR","capacidad":180,"disponibilidad":50}
        """.trimIndent()

        mockMvc.perform(post("/vuelos").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET vuelos id returns 404 when missing`() {
        mockMvc.perform(get("/vuelos/999")).andExpect(status().isNotFound)
    }

    private fun createVuelo(disponibilidad: Int): Long {
        val body = mockMvc.perform(
            post("/vuelos").contentType(MediaType.APPLICATION_JSON).content(vueloJson(disponibilidad = disponibilidad)),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return "\"id\":(\\d+)".toRegex().find(body)!!.groupValues[1].toLong()
    }

    @Test
    fun `GET vuelos with ids returns only those flights and ignores unknown ids`() {
        val first = createVuelo(10)
        createVuelo(20)
        val third = createVuelo(30)

        mockMvc.perform(get("/vuelos").param("ids", "$first,$third,99999"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.id == $first)].disponibilidad").value(10))
            .andExpect(jsonPath("$[?(@.id == $third)].disponibilidad").value(30))
    }

    @Test
    fun `GET vuelos with only unknown ids returns an empty list`() {
        createVuelo(10)

        mockMvc.perform(get("/vuelos").param("ids", "99999"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `GET vuelos with an empty ids list returns an empty list`() {
        createVuelo(10)

        mockMvc.perform(get("/vuelos").param("ids", ""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `GET vuelos without ids still returns every flight`() {
        createVuelo(10)
        createVuelo(20)

        mockMvc.perform(get("/vuelos"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `full crud flow through the HTTP layer`() {
        val createResult = mockMvc.perform(
            post("/vuelos").contentType(MediaType.APPLICATION_JSON).content(vueloJson()),
        ).andExpect(status().isCreated).andReturn()

        val id = "\"id\":(\\d+)".toRegex()
            .find(createResult.response.contentAsString)!!.groupValues[1]

        mockMvc.perform(get("/vuelos/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.origen").value("EZE"))

        mockMvc.perform(get("/vuelos"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(
            put("/vuelos/$id").contentType(MediaType.APPLICATION_JSON).content(vueloJson(disponibilidad = 10)),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.disponibilidad").value(10))

        mockMvc.perform(delete("/vuelos/$id")).andExpect(status().isNoContent)

        mockMvc.perform(get("/vuelos/$id")).andExpect(status().isNotFound)
    }
}
