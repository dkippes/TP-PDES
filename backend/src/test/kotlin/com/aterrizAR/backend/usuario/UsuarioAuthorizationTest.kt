package com.aterrizAR.backend.usuario

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsuarioAuthorizationTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var agenciaRepository: AgenciaRepository

    private val body = """{"nombre":"Ana","apellido":"Perez","correo":"ana@example.com","direccion":"Quilmes","rol":"COMPRADOR","password":"12345678"}"""

    private fun usuario(correo: String, perfil: Perfil) = usuarioRepository.saveAndFlush(Usuario(
        nombre = "Test", apellido = "User", correo = correo,
        direccion = "Direccion", passwordHash = "unused", perfil = perfil,
    ))

    private fun abmRequests(): List<MockHttpServletRequestBuilder> = listOf(
        get("/api/usuarios"),
        get("/api/usuarios/1"),
        post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(body),
        put("/api/usuarios/1").contentType(MediaType.APPLICATION_JSON).content(body),
        delete("/api/usuarios/1"),
    )

    private fun meRequests(): List<MockHttpServletRequestBuilder> = listOf(
        get("/api/usuarios/me"),
        put("/api/usuarios/me").contentType(MediaType.APPLICATION_JSON)
            .content("""{"nombre":"Ana","apellido":"Perez","direccion":"Quilmes"}"""),
        put("/api/usuarios/me/password").contentType(MediaType.APPLICATION_JSON)
            .content("""{"passwordActual":"incorrecta","passwordNueva":"nueva-clave"}"""),
    )

    private fun bearer(usuario: Usuario) = "Bearer ${jwtService.createToken(usuario)}"

    @Test
    fun `requests without a token are rejected with 401`() {
        (abmRequests() + meRequests()).forEach { request ->
            mockMvc.perform(request).andExpect(status().isUnauthorized)
        }
    }

    @Test
    fun `buyers and agents cannot use the user administration endpoints`() {
        val agencia = agenciaRepository.saveAndFlush(Agencia(nombre = "Agencia", email = "agency@example.com"))
        listOf(
            usuario("buyer@example.com", Comprador()),
            usuario("agent@example.com", Agente(agencia)),
        ).forEach { actor ->
            abmRequests().forEach { request ->
                mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(actor)))
                    .andExpect(status().isForbidden)
            }
        }
    }

    @Test
    fun `every role may use its own me endpoints`() {
        val agencia = agenciaRepository.saveAndFlush(Agencia(nombre = "Agencia", email = "agency@example.com"))
        listOf(
            usuario("buyer@example.com", Comprador()),
            usuario("agent@example.com", Agente(agencia)),
            usuario("admin@example.com", Administrador()),
        ).forEach { actor ->
            val (me, datos, password) = meRequests()
            mockMvc.perform(me.header(HttpHeaders.AUTHORIZATION, bearer(actor))).andExpect(status().isOk)
            mockMvc.perform(datos.header(HttpHeaders.AUTHORIZATION, bearer(actor))).andExpect(status().isOk)
            // Llega al servicio (400 por contraseña incorrecta), no lo frena la autorización.
            mockMvc.perform(password.header(HttpHeaders.AUTHORIZATION, bearer(actor))).andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `administrators may use the user administration endpoints`() {
        val admin = usuario("admin@example.com", Administrador())

        mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
            .andExpect(status().isOk)
    }
}
