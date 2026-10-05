package com.aterrizAR.backend.usuario

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.auth.RefreshTokenRepository
import com.aterrizAR.backend.auth.RefreshTokenService
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import com.aterrizAR.backend.model.Roles
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.paquete.PaqueteRepository
import com.aterrizAR.backend.security.UsuarioAutenticado
import java.time.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.server.ResponseStatusException

// Sin @Transactional: los tests de borrado y reemplazo de perfil necesitan que el servicio confirme su transacción.
@SpringBootTest
@ActiveProfiles("test")
class UsuarioServiceTest {

    @Autowired
    lateinit var usuarioService: UsuarioService

    @Autowired
    lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    lateinit var agenciaRepository: AgenciaRepository

    @Autowired
    lateinit var paqueteRepository: PaqueteRepository

    @Autowired
    lateinit var hotelRepository: HotelRepository

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    lateinit var refreshTokenService: RefreshTokenService

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    // Un administrador que no coincide con ningún usuario de prueba.
    private val administrador = UsuarioAutenticado(id = -1L, rol = Roles.ADMINISTRADOR)

    @AfterEach
    fun cleanUp() {
        jdbcTemplate.update("DELETE FROM compra")
        jdbcTemplate.update("DELETE FROM valoracion")
        refreshTokenRepository.deleteAll()
        usuarioRepository.deleteAll()
        paqueteRepository.deleteAll()
        agenciaRepository.deleteAll()
        hotelRepository.deleteAll()
    }

    private fun createRequest(
        correo: String = "ana@example.com",
        rol: String = Roles.COMPRADOR,
        agenciaId: Long? = null,
    ) = UsuarioCreateRequestDTO(
        nombre = "Ana", apellido = "Perez", correo = correo, direccion = "Quilmes",
        password = "12345678", rol = rol, agenciaId = agenciaId,
    )

    private fun updateRequest(
        correo: String = "ana@example.com",
        rol: String = Roles.COMPRADOR,
        agenciaId: Long? = null,
        password: String? = null,
    ) = UsuarioUpdateRequestDTO(
        nombre = "Ana Maria", apellido = "Gomez", correo = correo, direccion = "Bernal",
        rol = rol, agenciaId = agenciaId, password = password,
    )

    private fun agencia(email: String = "agencia@example.com") =
        agenciaRepository.save(Agencia(nombre = "Agencia", email = email))

    private fun paquete(agencia: Agencia): Paquete {
        val hotel = hotelRepository.save(Hotel(nombre = "Hotel", pais = "Argentina", foto = "foto"))
        return paqueteRepository.save(Paquete(
            precio = 100f, vueloIdaId = 1L, vueloVueltaId = 2L, hotel = hotel, agencia = agencia,
            destino = "Buenos Aires", origen = "Cordoba",
            fechaInicial = LocalDate.of(2026, 10, 1), fechaFinal = LocalDate.of(2026, 10, 5),
        ))
    }

    private fun valorar(comprador: Usuario) {
        jdbcTemplate.update(
            "INSERT INTO valoracion (puntaje, comentario, paquete_id, comprador_id) VALUES (?, ?, ?, ?)",
            5, "Excelente", paquete(agencia()).id, comprador.perfil.id,
        )
    }

    private fun valoracionCount() = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM valoracion", Int::class.java)

    private fun perfilCount() = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM perfil", Int::class.java)

    private fun activeTokens(usuario: Usuario) = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM refresh_token WHERE usuario_id = ? AND revoked_at IS NULL", Int::class.java, usuario.id,
    )

    @Test
    fun `create trims fields, normalizes the correo and hashes the password`() {
        val created = usuarioService.create(createRequest(correo = " Ana@Example.COM ").copy(nombre = "  Ana  "))

        assertEquals("Ana", created.nombre)
        assertEquals("ana@example.com", created.correo)
        assertEquals(Roles.COMPRADOR, created.perfil.roleName)
        assertNotEquals("12345678", created.passwordHash)
        assertTrue(passwordEncoder.matches("12345678", created.passwordHash))
    }

    @Test
    fun `create builds the profile for every role`() {
        val agencia = agencia()

        val admin = usuarioService.create(createRequest(correo = "admin@example.com", rol = Roles.ADMINISTRADOR))
        val agente = usuarioService.create(createRequest(correo = "agente@example.com", rol = Roles.AGENTE, agenciaId = agencia.id))

        assertEquals(Roles.ADMINISTRADOR, admin.perfil.roleName)
        assertEquals(Roles.AGENTE, agente.perfil.roleName)
        assertEquals(agencia.id, agente.toDTO().agenciaId)
    }

    @Test
    fun `create validates agenciaId against the role`() {
        val sinAgencia = assertThrows(ResponseStatusException::class.java) {
            usuarioService.create(createRequest(rol = Roles.AGENTE))
        }
        val agenciaSobrante = assertThrows(ResponseStatusException::class.java) {
            usuarioService.create(createRequest(agenciaId = agencia().id))
        }
        val agenciaInexistente = assertThrows(ResponseStatusException::class.java) {
            usuarioService.create(createRequest(rol = Roles.AGENTE, agenciaId = 999L))
        }

        assertEquals(400, sinAgencia.statusCode.value())
        assertEquals(400, agenciaSobrante.statusCode.value())
        assertEquals(404, agenciaInexistente.statusCode.value())
        assertEquals(0, usuarioRepository.count())
    }

    @Test
    fun `create rejects a correo that differs only in case with 409`() {
        usuarioService.create(createRequest())

        val exception = assertThrows(ResponseStatusException::class.java) {
            usuarioService.create(createRequest(correo = "ANA@example.com"))
        }

        assertEquals(409, exception.statusCode.value())
        assertEquals(1, usuarioRepository.count())
    }

    @Test
    fun `findAll filters by role in the database`() {
        usuarioService.create(createRequest(correo = "comprador@example.com"))
        usuarioService.create(createRequest(correo = "admin@example.com", rol = Roles.ADMINISTRADOR))
        usuarioService.create(createRequest(correo = "agente@example.com", rol = Roles.AGENTE, agenciaId = agencia().id))

        assertEquals(3, usuarioService.findAll(null).size)
        assertEquals(listOf("admin@example.com"), usuarioService.findAll(Roles.ADMINISTRADOR).map { it.correo })
        assertEquals(listOf("agente@example.com"), usuarioService.findAll(Roles.AGENTE).map { it.correo })
        assertEquals(listOf("comprador@example.com"), usuarioService.findAll(Roles.COMPRADOR).map { it.correo })
    }

    @Test
    fun `findAll rejects an unknown role with 400`() {
        val exception = assertThrows(ResponseStatusException::class.java) { usuarioService.findAll("SUPERUSUARIO") }

        assertEquals(400, exception.statusCode.value())
    }

    @Test
    fun `findById throws 404 when the usuario does not exist`() {
        val exception = assertThrows(ResponseStatusException::class.java) { usuarioService.findById(999L) }

        assertEquals(404, exception.statusCode.value())
    }

    @Test
    fun `update with the same role keeps profile, password and sessions`() {
        val created = usuarioService.create(createRequest())
        refreshTokenService.issueFor(created)

        val updated = usuarioService.update(created.id!!, updateRequest(correo = "Nueva@Example.com"), administrador)

        assertEquals("Ana Maria", updated.nombre)
        assertEquals("nueva@example.com", updated.correo)
        assertEquals(created.perfil.id, updated.perfil.id)
        assertTrue(passwordEncoder.matches("12345678", updated.passwordHash))
        assertEquals(1, activeTokens(updated))
    }

    @Test
    fun `update changing the role replaces the profile and revokes sessions`() {
        val created = usuarioService.create(createRequest())
        refreshTokenService.issueFor(created)
        val agencia = agencia()

        val updated = usuarioService.update(
            created.id!!, updateRequest(rol = Roles.AGENTE, agenciaId = agencia.id), administrador,
        )

        assertEquals(Roles.AGENTE, usuarioService.findById(created.id!!).perfil.roleName)
        assertNotEquals(created.perfil.id, updated.perfil.id)
        assertEquals(1, perfilCount())
        assertEquals(0, activeTokens(updated))
    }

    @Test
    fun `update moves an agent to another agency`() {
        val original = agencia("original@example.com")
        val nueva = agencia("nueva@example.com")
        val created = usuarioService.create(createRequest(rol = Roles.AGENTE, agenciaId = original.id))

        usuarioService.update(created.id!!, updateRequest(rol = Roles.AGENTE, agenciaId = nueva.id), administrador)

        assertEquals(nueva.id, usuarioService.findById(created.id!!).toDTO().agenciaId)
        assertEquals(1, perfilCount())
    }

    @Test
    fun `update with a password resets it and revokes sessions`() {
        val created = usuarioService.create(createRequest())
        refreshTokenService.issueFor(created)

        val updated = usuarioService.update(created.id!!, updateRequest(password = "nueva-clave"), administrador)

        assertTrue(passwordEncoder.matches("nueva-clave", updated.passwordHash))
        assertEquals(0, activeTokens(updated))
    }

    @Test
    fun `update rejects another user's correo and invalid agenciaId`() {
        usuarioService.create(createRequest(correo = "otro@example.com"))
        val created = usuarioService.create(createRequest())

        val duplicado = assertThrows(ResponseStatusException::class.java) {
            usuarioService.update(created.id!!, updateRequest(correo = "OTRO@example.com"), administrador)
        }
        val agenciaSobrante = assertThrows(ResponseStatusException::class.java) {
            usuarioService.update(created.id!!, updateRequest(agenciaId = agencia().id), administrador)
        }

        assertEquals(409, duplicado.statusCode.value())
        assertEquals(400, agenciaSobrante.statusCode.value())
        assertEquals("ana@example.com", usuarioService.findById(created.id!!).correo)
    }

    @Test
    fun `an administrator cannot remove its own administrator role`() {
        val admin = usuarioService.create(createRequest(rol = Roles.ADMINISTRADOR))
        val propio = UsuarioAutenticado(id = admin.id!!, rol = Roles.ADMINISTRADOR)

        val exception = assertThrows(ResponseStatusException::class.java) {
            usuarioService.update(admin.id!!, updateRequest(rol = Roles.COMPRADOR), propio)
        }
        usuarioService.update(admin.id!!, updateRequest(rol = Roles.ADMINISTRADOR), propio)

        assertEquals(409, exception.statusCode.value())
        assertEquals(Roles.ADMINISTRADOR, usuarioService.findById(admin.id!!).perfil.roleName)
    }

    @Test
    fun `update throws 404 when the usuario does not exist`() {
        assertThrows(ResponseStatusException::class.java) {
            usuarioService.update(999L, updateRequest(), administrador)
        }
    }

    @Test
    fun `delete removes the usuario, its profile and its sessions`() {
        val created = usuarioService.create(createRequest())
        refreshTokenService.issueFor(created)

        usuarioService.delete(created.id!!, administrador)

        assertEquals(0, usuarioRepository.count())
        assertEquals(0, perfilCount())
        assertEquals(0, refreshTokenRepository.count())
    }

    @Test
    fun `delete rejects deleting oneself with 409`() {
        val admin = usuarioService.create(createRequest(rol = Roles.ADMINISTRADOR))

        val exception = assertThrows(ResponseStatusException::class.java) {
            usuarioService.delete(admin.id!!, UsuarioAutenticado(id = admin.id!!, rol = Roles.ADMINISTRADOR))
        }

        assertEquals(409, exception.statusCode.value())
        assertEquals(1, usuarioRepository.count())
    }

    @Test
    fun `delete of a buyer with purchases returns 409 and keeps the usuario`() {
        val comprador = usuarioService.create(createRequest())
        val agencia = agencia()
        val paquete = paquete(agencia)
        jdbcTemplate.update(
            "INSERT INTO compra (precio, fecha, paquete_id, agencia_id, comprador_id) VALUES (?, ?, ?, ?, ?)",
            100f, LocalDate.of(2026, 9, 1), paquete.id, agencia.id, comprador.perfil.id,
        )

        val exception = assertThrows(ResponseStatusException::class.java) {
            usuarioService.delete(comprador.id!!, administrador)
        }

        assertEquals(409, exception.statusCode.value())
        assertEquals(1, usuarioRepository.count())
    }

    @Test
    fun `delete of a buyer with reviews returns 409 and keeps the reviews`() {
        val comprador = usuarioService.create(createRequest())
        valorar(comprador)

        val exception = assertThrows(ResponseStatusException::class.java) {
            usuarioService.delete(comprador.id!!, administrador)
        }

        assertEquals(409, exception.statusCode.value())
        assertEquals(1, usuarioRepository.count())
        assertEquals(1, valoracionCount())
    }

    @Test
    fun `changing the role of a buyer with reviews returns 409 and keeps role and reviews`() {
        val comprador = usuarioService.create(createRequest())
        valorar(comprador)

        val exception = assertThrows(ResponseStatusException::class.java) {
            usuarioService.update(comprador.id!!, updateRequest(rol = Roles.ADMINISTRADOR), administrador)
        }

        assertEquals(409, exception.statusCode.value())
        val persisted = usuarioService.findById(comprador.id!!)
        assertEquals(Roles.COMPRADOR, persisted.perfil.roleName)
        assertEquals("Ana", persisted.nombre)
        assertEquals(1, valoracionCount())
    }

    @Test
    fun `delete throws 404 when the usuario does not exist`() {
        val exception = assertThrows(ResponseStatusException::class.java) { usuarioService.delete(999L, administrador) }

        assertEquals(404, exception.statusCode.value())
    }

    @Test
    fun `findActual returns the authenticated usuario`() {
        val created = usuarioService.create(createRequest())

        val actual = usuarioService.findActual(UsuarioAutenticado(id = created.id!!, rol = Roles.COMPRADOR))

        assertEquals(created.id, actual.id)
    }

    @Test
    fun `actualizarDatosPersonales changes only personal data`() {
        val created = usuarioService.create(createRequest())
        val propio = UsuarioAutenticado(id = created.id!!, rol = Roles.COMPRADOR)

        usuarioService.actualizarDatosPersonales(propio, DatosPersonalesRequestDTO(" Ana Maria ", "Gomez", "Bernal"))

        val persisted = usuarioService.findById(created.id!!)
        assertEquals("Ana Maria", persisted.nombre)
        assertEquals("Gomez", persisted.apellido)
        assertEquals("Bernal", persisted.direccion)
        assertEquals("ana@example.com", persisted.correo)
        assertEquals(created.perfil.id, persisted.perfil.id)
    }

    @Test
    fun `cambiarPassword requires the current password and revokes sessions`() {
        val created = usuarioService.create(createRequest())
        refreshTokenService.issueFor(created)
        val propio = UsuarioAutenticado(id = created.id!!, rol = Roles.COMPRADOR)

        val incorrecta = assertThrows(ResponseStatusException::class.java) {
            usuarioService.cambiarPassword(propio, CambioPasswordRequestDTO("incorrecta", "nueva-clave"))
        }
        assertEquals(400, incorrecta.statusCode.value())
        assertEquals(1, activeTokens(created))

        usuarioService.cambiarPassword(propio, CambioPasswordRequestDTO("12345678", "nueva-clave"))

        val persisted = usuarioService.findById(created.id!!)
        assertTrue(passwordEncoder.matches("nueva-clave", persisted.passwordHash))
        assertFalse(passwordEncoder.matches("12345678", persisted.passwordHash))
        assertEquals(0, activeTokens(persisted))
        assertNull(persisted.toDTO().agenciaId)
    }
}
