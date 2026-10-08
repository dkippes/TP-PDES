package com.aterrizAR.backend.auth

import com.aterrizAR.backend.model.Roles
import com.aterrizAR.backend.usuario.UsuarioRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "dev")
@Transactional
class DevUsersSeederTest {
    @Autowired lateinit var seeder: DevUsersSeeder
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder
    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun `startup creates demo accounts with hashed passwords and login roles`() {
        for ((correo, role) in mapOf(
            "admin@gmail.com" to Roles.ADMINISTRADOR,
            "usuario@gmail.com" to Roles.COMPRADOR,
        )) {
            val usuario = requireNotNull(usuarioRepository.findByCorreo(correo))
            assertEquals(role, usuario.perfil.roleName)
            assertNotEquals("12345678", usuario.passwordHash)
            assertTrue(passwordEncoder.matches("12345678", usuario.passwordHash))

            mockMvc.perform(
                post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("""{"correo":"$correo","password":"12345678"}"""),
            ).andExpect(status().isOk)
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value(role))
        }
    }

    @Test
    fun `repeated startup preserves accounts and their password hashes`() {
        val original = listOf("admin@gmail.com", "usuario@gmail.com").associateWith {
            requireNotNull(usuarioRepository.findByCorreo(it))
        }
        val count = usuarioRepository.count()

        seeder.run()
        seeder.run()

        assertEquals(count, usuarioRepository.count())
        for ((correo, usuario) in original) {
            val current = requireNotNull(usuarioRepository.findByCorreo(correo))
            assertEquals(usuario.id, current.id)
            assertEquals(usuario.passwordHash, current.passwordHash)
        }
    }

    @Test
    fun `deleted demo account is recreated without duplicating the other one`() {
        val adminId = requireNotNull(usuarioRepository.findByCorreo("admin@gmail.com")).id
        usuarioRepository.delete(requireNotNull(usuarioRepository.findByCorreo("usuario@gmail.com")))
        usuarioRepository.flush()

        seeder.run()

        assertEquals(adminId, requireNotNull(usuarioRepository.findByCorreo("admin@gmail.com")).id)
        val recreated = requireNotNull(usuarioRepository.findByCorreo("usuario@gmail.com"))
        assertEquals(Roles.COMPRADOR, recreated.perfil.roleName)
        assertTrue(passwordEncoder.matches("12345678", recreated.passwordHash))
        assertEquals(2L, usuarioRepository.count())
    }

    @Test
    fun `production profile does not register the demo seeder`() {
        AnnotationConfigApplicationContext().use { context ->
            context.environment.setActiveProfiles("prod")
            context.register(DevUsersSeeder::class.java)
            context.refresh()
            assertTrue(context.getBeansOfType(DevUsersSeeder::class.java).isEmpty())
        }
    }
}
