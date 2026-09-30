package com.aterrizAR.backend.hotel

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
class HotelControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var hotelRepository: HotelRepository

    @AfterEach
    fun cleanUp() {
        hotelRepository.deleteAll()
    }

    private fun hotelJson(nombre: String = "Sheraton") = """
        {"nombre":"$nombre","pais":"Argentina","foto":"https://example.com/sheraton.jpg"}
    """.trimIndent()

    @Test
    fun `POST hoteles creates a hotel`() {
        mockMvc.perform(post("/api/hoteles").contentType(MediaType.APPLICATION_JSON).content(hotelJson()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.nombre").value("Sheraton"))
    }

    @Test
    fun `POST hoteles rejects blank required fields`() {
        val invalidJson = """{"nombre":"","pais":"Argentina","foto":"https://example.com/sheraton.jpg"}"""

        mockMvc.perform(post("/api/hoteles").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET hoteles id returns 404 when missing`() {
        mockMvc.perform(get("/api/hoteles/999")).andExpect(status().isNotFound)
    }

    @Test
    fun `full crud flow through the HTTP layer`() {
        val createResult = mockMvc.perform(
            post("/api/hoteles").contentType(MediaType.APPLICATION_JSON).content(hotelJson()),
        ).andExpect(status().isCreated).andReturn()

        val id = "\"id\":(\\d+)".toRegex()
            .find(createResult.response.contentAsString)!!.groupValues[1]

        mockMvc.perform(get("/api/hoteles/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nombre").value("Sheraton"))

        mockMvc.perform(get("/api/hoteles"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(
            put("/api/hoteles/$id").contentType(MediaType.APPLICATION_JSON).content(hotelJson(nombre = "Hilton")),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.nombre").value("Hilton"))

        mockMvc.perform(delete("/api/hoteles/$id")).andExpect(status().isNoContent)

        mockMvc.perform(get("/api/hoteles/$id")).andExpect(status().isNotFound)
    }
}
