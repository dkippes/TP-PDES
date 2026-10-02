package com.aterrizAR.backend.agencia

import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.security.authenticatedAs
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgenciaControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var agenciaRepository: AgenciaRepository

    @Autowired
    lateinit var jwtService: JwtService

    private fun perform(request: MockHttpServletRequestBuilder) =
        mockMvc.perform(request.authenticatedAs(jwtService))

    @AfterEach
    fun cleanUp() {
        agenciaRepository.deleteAll()
    }

    private fun agenciaJson(nombre: String = "Sheraton") = """
        {"nombre":"$nombre","email":"agencia@example.com"}
    """.trimIndent()

    @Test
    fun `POST and PUT reject invalid fields`() {
        val agencia = agenciaRepository.save(com.aterrizAR.backend.model.Agencia(nombre = "Agencia", email = "valid@example.com"))
        for (body in listOf(
            """{"nombre":"Agencia","email":""}""",
            """{"nombre":"Agencia","email":"invalid"}""",
            """{"nombre":"${"a".repeat(256)}","email":"valid@example.com"}""",
        )) {
            perform(post("/api/agencias").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
            perform(put("/api/agencias/${agencia.id}").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `duplicate email returns 409 on create and update`() {
        val first = agenciaRepository.save(com.aterrizAR.backend.model.Agencia(nombre = "Primera", email = "agencia@example.com"))
        val second = agenciaRepository.save(com.aterrizAR.backend.model.Agencia(nombre = "Segunda", email = "second@example.com"))
        perform(post("/api/agencias").contentType(MediaType.APPLICATION_JSON).content(agenciaJson()))
            .andExpect(status().isConflict)
        perform(put("/api/agencias/${second.id}").contentType(MediaType.APPLICATION_JSON).content(agenciaJson()))
            .andExpect(status().isConflict)
        perform(get("/api/agencias/${second.id}"))
            .andExpect(jsonPath("$.email").value("second@example.com"))
        perform(put("/api/agencias/${first.id}").contentType(MediaType.APPLICATION_JSON).content(agenciaJson()))
            .andExpect(status().isOk)
    }

    @Test
    fun `update and delete missing agency return 404`() {
        perform(put("/api/agencias/999").contentType(MediaType.APPLICATION_JSON).content(agenciaJson()))
            .andExpect(status().isNotFound)
        perform(delete("/api/agencias/999")).andExpect(status().isNotFound)
    }

    @Test
    fun `POST agencias creates a agencia`() {
        perform(post("/api/agencias").contentType(MediaType.APPLICATION_JSON).content(agenciaJson()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.nombre").value("Sheraton"))
            .andExpect(jsonPath("$.email").value("agencia@example.com"))
            .andExpect(jsonPath("$.paquetes").doesNotExist())
            .andExpect(jsonPath("$.compras").doesNotExist())
    }

    @Test
    fun `POST agencias rejects blank required fields`() {
        val invalidJson = """{"nombre":"","email":"agencia@example.com"}"""

        perform(post("/api/agencias").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET agencias id returns 404 when missing`() {
        perform(get("/api/agencias/999")).andExpect(status().isNotFound)
    }

    @Test
    fun `full crud flow through the HTTP layer`() {
        val createResult = perform(
            post("/api/agencias").contentType(MediaType.APPLICATION_JSON).content(agenciaJson()),
        ).andExpect(status().isCreated).andReturn()

        val id = "\"id\":(\\d+)".toRegex()
            .find(createResult.response.contentAsString)!!.groupValues[1]

        perform(get("/api/agencias/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nombre").value("Sheraton"))

        perform(get("/api/agencias"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        perform(
            put("/api/agencias/$id").contentType(MediaType.APPLICATION_JSON).content(agenciaJson(nombre = "Hilton")),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.nombre").value("Hilton"))

        perform(delete("/api/agencias/$id")).andExpect(status().isNoContent)

        perform(get("/api/agencias/$id")).andExpect(status().isNotFound)
    }
}
