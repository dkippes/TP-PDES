package com.aterrizAR.backend.observability

import com.aterrizAR.backend.auth.AuthService
import com.aterrizAR.backend.auth.DuplicateEmailException
import com.aterrizAR.backend.auth.InvalidCredentialsException
import com.aterrizAR.backend.auth.LoginRequestDTO
import com.aterrizAR.backend.auth.RefreshTokenRepository
import com.aterrizAR.backend.auth.RegisterRequestDTO
import com.aterrizAR.backend.usuario.UsuarioRepository
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NegocioMetricsTest {
    @Autowired lateinit var registry: MeterRegistry
    @Autowired lateinit var authService: AuthService
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository

    @AfterEach
    fun cleanUp() {
        refreshTokenRepository.deleteAll()
        usuarioRepository.deleteAll()
    }

    private fun count(name: String, resultado: String) = registry.get(name).tag("resultado", resultado).counter().count()

    @Test
    fun `registration and login outcomes are counted without identifying tags`() {
        val registros = count("aterrizar.registros", "exitoso")
        val duplicados = count("aterrizar.registros", "duplicado")
        val ok = count("aterrizar.logins", "exitoso")
        val fallidos = count("aterrizar.logins", "fallido")
        val request = RegisterRequestDTO("Ana", "Pérez", "ana@example.com", "Calle 1", "password123")

        authService.register(request)
        assertThrows(DuplicateEmailException::class.java) { authService.register(request) }
        authService.login(LoginRequestDTO("ana@example.com", "password123"))
        assertThrows(InvalidCredentialsException::class.java) { authService.login(LoginRequestDTO("ana@example.com", "wrong-password")) }

        assertEquals(registros + 1, count("aterrizar.registros", "exitoso"))
        assertEquals(duplicados + 1, count("aterrizar.registros", "duplicado"))
        assertEquals(ok + 1, count("aterrizar.logins", "exitoso"))
        assertEquals(fallidos + 1, count("aterrizar.logins", "fallido"))
        assertEquals(1.0, registry.get("aterrizar.usuarios.total").gauge().value())
    }

    @Test
    fun `prometheus endpoint is public and exposes business and http metrics`() {
        val body = mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        assertTrue(body.contains("aterrizar_compras_total{"))
        assertTrue(body.contains("aterrizar_logins_total{"))
        assertFalse(body.contains("ana@example.com"))
    }

    @Test
    fun `only health and prometheus are exposed`() {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk)
        // Kubernetes probes llegan sin credenciales
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk)
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized)
    }
}
