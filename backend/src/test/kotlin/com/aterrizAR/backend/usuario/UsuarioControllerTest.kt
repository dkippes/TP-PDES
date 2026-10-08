package com.aterrizAR.backend.usuario

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.auth.RefreshTokenRepository
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
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
class UsuarioControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    lateinit var agenciaRepository: AgenciaRepository

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    lateinit var jwtService: JwtService

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    private lateinit var admin: Usuario

    // El actor es un usuario persistido: /me y las protecciones sobre uno mismo dependen de su id real.
    @BeforeEach
    fun setUp() {
        admin = usuario("admin-actor@example.com", Administrador())
    }

    @AfterEach
    fun cleanUp() {
        refreshTokenRepository.deleteAll()
        usuarioRepository.deleteAll()
        agenciaRepository.deleteAll()
    }

    private fun usuario(correo: String, perfil: Perfil) = usuarioRepository.save(Usuario(
        nombre = "Test", apellido = "User", correo = correo, direccion = "Quilmes",
        passwordHash = requireNotNull(passwordEncoder.encode("12345678")), perfil = perfil,
    ))

    private fun perform(request: MockHttpServletRequestBuilder, actor: Usuario = admin) = mockMvc.perform(
        request.header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtService.createToken(actor)}"),
    )

    private fun json(request: MockHttpServletRequestBuilder, body: String) =
        request.contentType(MediaType.APPLICATION_JSON).content(body)

    private fun usuarioJson(
        correo: String = "ana@example.com",
        rol: String = "COMPRADOR",
        extra: String = ""","password":"12345678"""",
    ) = """{"nombre":"Ana","apellido":"Perez","correo":"$correo","direccion":"Quilmes","rol":"$rol"$extra}"""

    @Test
    fun `POST usuarios creates a usuario without exposing the password`() {
        perform(json(post("/api/usuarios"), usuarioJson()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.correo").value("ana@example.com"))
            .andExpect(jsonPath("$.rol").value("COMPRADOR"))
            .andExpect(jsonPath("$.agenciaId").isEmpty)
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
    }

    @Test
    fun `POST usuarios creates an agent linked to its agency`() {
        val agencia = agenciaRepository.save(Agencia(nombre = "Agencia", email = "agencia@example.com"))

        perform(json(post("/api/usuarios"), usuarioJson(rol = "AGENTE", extra = ""","password":"12345678","agenciaId":${agencia.id}""")))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.rol").value("AGENTE"))
            .andExpect(jsonPath("$.agenciaId").value(agencia.id!!))
    }

    @Test
    fun `POST and PUT reject invalid fields`() {
        val existente = usuario("existente@example.com", Comprador())
        for (body in listOf(
            usuarioJson(correo = "invalido"),
            usuarioJson(rol = "SUPERUSUARIO"),
            usuarioJson(extra = ""","password":"corta""""),
            """{"nombre":"","apellido":"Perez","correo":"ana@example.com","direccion":"Quilmes","rol":"COMPRADOR","password":"12345678"}""",
        )) {
            perform(json(post("/api/usuarios"), body)).andExpect(status().isBadRequest)
            perform(json(put("/api/usuarios/${existente.id}"), body)).andExpect(status().isBadRequest)
        }
        perform(json(post("/api/usuarios"), usuarioJson(extra = ""))).andExpect(status().isBadRequest)
        perform(json(post("/api/usuarios"), usuarioJson(rol = "AGENTE"))).andExpect(status().isBadRequest)
        perform(get("/api/usuarios").param("rol", "SUPERUSUARIO")).andExpect(status().isBadRequest)
    }

    @Test
    fun `duplicate correo returns 409`() {
        usuario("ana@example.com", Comprador())

        perform(json(post("/api/usuarios"), usuarioJson(correo = "ANA@example.com"))).andExpect(status().isConflict)
    }

    @Test
    fun `missing usuario returns 404`() {
        perform(get("/api/usuarios/999")).andExpect(status().isNotFound)
        perform(json(put("/api/usuarios/999"), usuarioJson(extra = ""))).andExpect(status().isNotFound)
        perform(delete("/api/usuarios/999")).andExpect(status().isNotFound)
    }

    @Test
    fun `an administrator cannot delete or demote itself`() {
        perform(delete("/api/usuarios/${admin.id}")).andExpect(status().isConflict)
        perform(json(put("/api/usuarios/${admin.id}"), usuarioJson(correo = admin.correo, extra = "")))
            .andExpect(status().isConflict)
    }

    @Test
    fun `full crud flow through the HTTP layer`() {
        val createResult = perform(json(post("/api/usuarios"), usuarioJson()))
            .andExpect(status().isCreated).andReturn()
        val id = "\"id\":(\\d+)".toRegex().find(createResult.response.contentAsString)!!.groupValues[1]

        perform(get("/api/usuarios/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nombre").value("Ana"))

        perform(get("/api/usuarios").param("rol", "COMPRADOR"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        perform(json(put("/api/usuarios/$id"), usuarioJson(rol = "ADMINISTRADOR", extra = ""","password":"nueva-clave"""")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
        assertTrue(passwordEncoder.matches("nueva-clave", usuarioRepository.findById(id.toLong()).orElseThrow().passwordHash))

        perform(delete("/api/usuarios/$id")).andExpect(status().isNoContent)
        perform(get("/api/usuarios/$id")).andExpect(status().isNotFound)
    }

    @Test
    fun `me endpoints read and update the authenticated usuario`() {
        val comprador = usuario("comprador@example.com", Comprador())

        perform(get("/api/usuarios/me"), comprador)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(comprador.id!!))
            .andExpect(jsonPath("$.rol").value("COMPRADOR"))

        perform(json(put("/api/usuarios/me"), """{"nombre":"Nuevo","apellido":"Nombre","direccion":"Bernal"}"""), comprador)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nombre").value("Nuevo"))
            .andExpect(jsonPath("$.correo").value("comprador@example.com"))
        perform(json(put("/api/usuarios/me"), """{"nombre":"","apellido":"Nombre","direccion":"Bernal"}"""), comprador)
            .andExpect(status().isBadRequest)

        perform(json(put("/api/usuarios/me/password"), """{"passwordActual":"incorrecta","passwordNueva":"nueva-clave"}"""), comprador)
            .andExpect(status().isBadRequest)
        perform(json(put("/api/usuarios/me/password"), """{"passwordActual":"12345678","passwordNueva":"nueva-clave"}"""), comprador)
            .andExpect(status().isNoContent)
        assertTrue(passwordEncoder.matches("nueva-clave", usuarioRepository.findById(comprador.id!!).orElseThrow().passwordHash))
    }
}
