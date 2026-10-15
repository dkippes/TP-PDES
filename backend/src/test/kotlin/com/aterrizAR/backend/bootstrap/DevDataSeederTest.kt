package com.aterrizAR.backend.bootstrap

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.model.Agente
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

@SpringBootTest(properties = ["spring.datasource.url=jdbc:h2:mem:dev_users_seed_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"])
@AutoConfigureMockMvc
@ActiveProfiles("test", "dev")
@Transactional
class DevDataSeederTest {
    @Autowired lateinit var seeder: DevDataSeeder
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var agenciaRepository: AgenciaRepository

    @Test
    fun `startup creates demo accounts with hashed passwords and login roles`() {
        for ((correo, role) in mapOf(
            "admin@gmail.com" to Roles.ADMINISTRADOR,
            "usuario@gmail.com" to Roles.COMPRADOR,
            "agente@gmail.com" to Roles.AGENTE,
            "agente.andino@gmail.com" to Roles.AGENTE,
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
        val count = usuarioRepository.count()
        val adminId = requireNotNull(usuarioRepository.findByCorreo("admin@gmail.com")).id
        usuarioRepository.delete(requireNotNull(usuarioRepository.findByCorreo("usuario@gmail.com")))
        usuarioRepository.flush()

        seeder.run()

        assertEquals(adminId, requireNotNull(usuarioRepository.findByCorreo("admin@gmail.com")).id)
        val recreated = requireNotNull(usuarioRepository.findByCorreo("usuario@gmail.com"))
        assertEquals(Roles.COMPRADOR, recreated.perfil.roleName)
        assertTrue(passwordEncoder.matches("12345678", recreated.passwordHash))
        assertEquals(count, usuarioRepository.count())
    }

    @Test
    fun `startup links demo agents to agencies and repeated startup preserves edited data`() {
        val sur = requireNotNull(agenciaRepository.findByEmail("contacto@viajesdelsur.example"))
        val andina = requireNotNull(agenciaRepository.findByEmail("contacto@rumboandino.example"))
        val agenteSur = requireNotNull(usuarioRepository.findByCorreo("agente@gmail.com"))
        val agenteAndino = requireNotNull(usuarioRepository.findByCorreo("agente.andino@gmail.com"))
        assertEquals(sur.id, (agenteSur.perfil as Agente).agencia.id)
        assertEquals(andina.id, (agenteAndino.perfil as Agente).agencia.id)
        assertTrue(agenciaRepository.findByEmail("contacto@horizonte.example") != null)
        val agencyCount = agenciaRepository.count()
        val userCount = usuarioRepository.count()
        val passwordHash = agenteSur.passwordHash
        sur.nombre = "Nombre editado"
        agenteSur.nombre = "Agente editado"
        agenciaRepository.saveAndFlush(sur)
        usuarioRepository.saveAndFlush(agenteSur)

        seeder.run()
        seeder.run()

        assertEquals(agencyCount, agenciaRepository.count())
        assertEquals(userCount, usuarioRepository.count())
        assertEquals("Nombre editado", requireNotNull(agenciaRepository.findByEmail(sur.email)).nombre)
        val current = requireNotNull(usuarioRepository.findByCorreo(agenteSur.correo))
        assertEquals(agenteSur.id, current.id)
        assertEquals("Agente editado", current.nombre)
        assertEquals(passwordHash, current.passwordHash)
    }

    @Test
    fun `production profile does not register the demo seeder`() {
        AnnotationConfigApplicationContext().use { context ->
            context.environment.setActiveProfiles("prod")
            context.register(DevDataSeeder::class.java)
            context.refresh()
            assertTrue(context.getBeansOfType(DevDataSeeder::class.java).isEmpty())
        }
    }
}
