package com.aterrizAR.backend.auth

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {
    @Autowired lateinit var authService: AuthService
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    @AfterEach
    fun cleanUp() {
        refreshTokenRepository.deleteAll()
        usuarioRepository.deleteAll()
    }

    private fun registration() = RegisterRequestDTO("Ana", "Pérez", "ana@example.com", "Calle 1", "password123")

    @Test
    fun `register persists a buyer with a hashed password`() {
        val user = authService.register(registration())

        assertEquals("COMPRADOR", user.role)
        assertEquals("ana@example.com", user.correo)
        val saved = usuarioRepository.findByCorreo(user.correo)!!
        assertEquals(user.id, saved.id)
        assertNotEquals("password123", saved.passwordHash)
        assertTrue(passwordEncoder.matches("password123", saved.passwordHash))
    }

    @Test
    fun `register rejects duplicate email`() {
        authService.register(registration())

        assertThrows(DuplicateEmailException::class.java) {
            authService.register(registration().copy(correo = " ANA@example.com "))
        }
        assertEquals(1, usuarioRepository.count())
    }

    @Test
    fun `login returns an access token and refresh session for valid credentials`() {
        val registered = authService.register(registration())

        val session = authService.login(LoginRequestDTO(" ANA@example.com ", "password123"))

        assertEquals(registered.id, session.response.user.id)
        assertEquals("Bearer", session.response.tokenType)
        assertTrue(session.response.accessToken.isNotBlank())
        assertTrue(session.rawRefreshToken.isNotBlank())
        assertEquals(1, refreshTokenRepository.count())
    }

    @Test
    fun `login rejects unknown email and incorrect password without issuing tokens`() {
        authService.register(registration())

        assertThrows(InvalidCredentialsException::class.java) {
            authService.login(LoginRequestDTO("unknown@example.com", "password123"))
        }
        assertThrows(InvalidCredentialsException::class.java) {
            authService.login(LoginRequestDTO("ana@example.com", "wrongpass123"))
        }
        assertFalse(refreshTokenRepository.count() > 0)
    }
}
