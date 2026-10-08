package com.aterrizAR.backend.agencia

import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.auth.UsuarioRepository
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Usuario
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
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

    @Autowired
    lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    // El permiso de update se resuelve con el perfil persistido, así que el administrador debe existir.
    private lateinit var administrador: Usuario

    @BeforeEach
    fun createAdministrador() {
        administrador = usuarioRepository.save(Usuario(
            nombre = "Admin", apellido = "Test", correo = "agency-controller-admin@example.com",
            direccion = "Direccion", passwordHash = "unused", perfil = Administrador(),
        ))
    }

    @AfterEach
    fun cleanUp() {
        agenciaRepository.deleteAll()
        usuarioRepository.deleteById(administrador.id!!)
        jdbcTemplate.update("DELETE FROM administrador WHERE id = ?", administrador.perfil.id)
        jdbcTemplate.update("DELETE FROM perfil WHERE id = ?", administrador.perfil.id)
    }

    private fun perform(request: MockHttpServletRequestBuilder) = mockMvc.perform(
        request.header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtService.createToken(administrador)}"),
    )

    private fun agenciaJson(nombre: String = "Viajes Sur") = """
        {"nombre":"$nombre","email":"ventas@viajessur.com"}
    """.trimIndent()

    @Test
    fun `POST agencias creates an agencia`() {
        perform(post("/api/agencias").contentType(MediaType.APPLICATION_JSON).content(agenciaJson()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.nombre").value("Viajes Sur"))
            .andExpect(jsonPath("$.email").value("ventas@viajessur.com"))
            .andExpect(jsonPath("$.paquetes").doesNotExist())
            .andExpect(jsonPath("$.compras").doesNotExist())
    }

    @Test
    fun `POST and PUT reject invalid fields`() {
        val agencia = agenciaRepository.save(Agencia(nombre = "Viajes Sur", email = "valid@example.com"))
        for (body in listOf(
            """{"nombre":"","email":"valid@example.com"}""",
            """{"nombre":"${"a".repeat(256)}","email":"valid@example.com"}""",
            """{"nombre":"Viajes Sur","email":""}""",
            """{"nombre":"Viajes Sur","email":"invalid"}""",
        )) {
            perform(post("/api/agencias").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
            perform(put("/api/agencias/${agencia.id}").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `duplicate email returns 409 on create and update`() {
        val first = agenciaRepository.save(Agencia(nombre = "Primera", email = "ventas@viajessur.com"))
        val second = agenciaRepository.save(Agencia(nombre = "Segunda", email = "second@example.com"))
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
    fun `GET agencias id returns 404 when missing`() {
        perform(get("/api/agencias/999")).andExpect(status().isNotFound)
    }

    @Test
    fun `update and delete missing agency return 404`() {
        perform(put("/api/agencias/999").contentType(MediaType.APPLICATION_JSON).content(agenciaJson()))
            .andExpect(status().isNotFound)
        perform(delete("/api/agencias/999")).andExpect(status().isNotFound)
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
            .andExpect(jsonPath("$.nombre").value("Viajes Sur"))

        perform(get("/api/agencias"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        perform(
            put("/api/agencias/$id").contentType(MediaType.APPLICATION_JSON).content(agenciaJson(nombre = "Viajes Norte")),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.nombre").value("Viajes Norte"))

        perform(delete("/api/agencias/$id")).andExpect(status().isNoContent)

        perform(get("/api/agencias/$id")).andExpect(status().isNotFound)
    }
}
