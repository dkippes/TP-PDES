package com.aterrizAR.backend.auth

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository

    @AfterEach
    fun cleanUp() {
        refreshTokenRepository.deleteAll()
        usuarioRepository.deleteAll()
    }

    private fun registerJson(password: String = "password123") = """
        {"nombre":"Ana","apellido":"Pérez","correo":"ana@example.com","direccion":"Calle 1","password":"$password"}
    """.trimIndent()

    private fun loginJson(password: String = "password123") = """
        {"correo":"ana@example.com","password":"$password"}
    """.trimIndent()

    private fun register() = mockMvc.perform(
        post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson()),
    ).andExpect(status().isCreated)

    @Test
    fun `register creates buyer and rejects duplicate email`() {
        register().andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.role").value("COMPRADOR"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson()))
            .andExpect(status().isConflict)
    }

    @Test
    fun `register and login validate request bodies`() {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson("short")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors.password").exists())
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("""{"correo":"invalid","password":"password123"}"""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `login returns access token and refresh cookie but rejects bad credentials`() {
        register()

        val response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.user.correo").value("ana@example.com"))
            .andReturn().response
        assertNotNull(response.getCookie("refresh_token"))

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson("wrongpass123")))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `refresh rotates cookie and logout revokes it`() {
        register()
        val loginCookie = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson()))
            .andExpect(status().isOk).andReturn().response.cookies.single()

        val refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(loginCookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn().response.getCookie(loginCookie.name)!!
        assertNotEquals(loginCookie.value, refreshed.value)

        mockMvc.perform(post("/api/auth/logout").cookie(refreshed))
            .andExpect(status().isOk)
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshed))
            .andExpect(status().isUnauthorized)
    }
}
