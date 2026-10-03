package com.aterrizAR.backend.agencia

import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.auth.UsuarioRepository
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AgenciaAuthorizationTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var agenciaRepository: AgenciaRepository

    private val body = """{"nombre":"Actualizada","email":"updated@example.com"}"""

    private fun agencia(email: String) = agenciaRepository.saveAndFlush(Agencia(nombre = "Original", email = email))

    private fun usuario(perfil: Perfil) = usuarioRepository.saveAndFlush(Usuario(
        nombre = "Test", apellido = "User", correo = "agency-permissions@example.com",
        direccion = "Direccion", passwordHash = "unused", perfil = perfil,
    ))

    private fun update(id: Long, token: String) = mockMvc.perform(
        put("/api/agencias/$id").header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON).content(body),
    )

    @Test
    fun `agent may update its own agency`() {
        val propia = agencia("own@example.com")
        val token = jwtService.createToken(usuario(Agente(propia)))
        update(propia.id!!, token).andExpect(status().isOk)
        assertEquals("Actualizada", agenciaRepository.findById(propia.id!!).orElseThrow().nombre)
    }

    @Test
    fun `agent cannot update another agency and leaves it unchanged`() {
        val propia = agencia("own@example.com")
        val otra = agencia("other@example.com")
        val token = jwtService.createToken(usuario(Agente(propia)))
        update(otra.id!!, token).andExpect(status().isForbidden)
        assertEquals("Original", agenciaRepository.findById(otra.id!!).orElseThrow().nombre)
        assertEquals("other@example.com", agenciaRepository.findById(otra.id!!).orElseThrow().email)
    }

    @Test
    fun `agent gets 403 rather than 404 for a missing agency`() {
        val propia = agencia("own@example.com")
        val token = jwtService.createToken(usuario(Agente(propia)))
        update(999L, token).andExpect(status().isForbidden)
    }

    @Test
    fun `administrator may update any agency`() {
        val otra = agencia("other@example.com")
        val token = jwtService.createToken(usuario(Administrador()))
        update(otra.id!!, token).andExpect(status().isOk)
    }

    @Test
    fun `agent cannot create or delete agencies`() {
        val propia = agencia("own@example.com")
        val token = jwtService.createToken(usuario(Agente(propia)))
        mockMvc.perform(post("/api/agencias").header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden)
        mockMvc.perform(delete("/api/agencias/${propia.id}")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")).andExpect(status().isForbidden)
    }

    @Test
    fun `buyer cannot update agencies`() {
        val otra = agencia("other@example.com")
        val token = jwtService.createToken(usuario(Comprador()))
        update(otra.id!!, token).andExpect(status().isForbidden)
    }

    @Test
    fun `agent token cannot update after its user was deleted`() {
        val propia = agencia("own@example.com")
        val agente = usuario(Agente(propia))
        val token = jwtService.createToken(agente)
        usuarioRepository.delete(agente)
        usuarioRepository.flush()
        update(propia.id!!, token).andExpect(status().isForbidden)
    }
}
