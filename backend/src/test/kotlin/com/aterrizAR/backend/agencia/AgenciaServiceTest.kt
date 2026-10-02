package com.aterrizAR.backend.agencia

import com.aterrizAR.backend.auth.UsuarioRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.paquete.PaqueteRepository
import java.time.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.server.ResponseStatusException

@SpringBootTest
@ActiveProfiles("test")
class AgenciaServiceTest {

    @Autowired
    lateinit var agenciaService: AgenciaService

    @Autowired
    lateinit var agenciaRepository: AgenciaRepository

    @Autowired lateinit var paqueteRepository: PaqueteRepository
    @Autowired lateinit var hotelRepository: HotelRepository
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private var agenteUsuarioId: Long? = null
    private var agentePerfilId: Long? = null

    @BeforeEach
    fun authenticateAdministrator() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            "1", null, listOf(SimpleGrantedAuthority("ROLE_ADMINISTRADOR")),
        )
    }

    @AfterEach
    fun cleanUp() {
        SecurityContextHolder.clearContext()
        agenteUsuarioId?.let { usuarioRepository.deleteById(it) }
        agentePerfilId?.let {
            jdbcTemplate.update("DELETE FROM agente WHERE id = ?", it)
            jdbcTemplate.update("DELETE FROM perfil WHERE id = ?", it)
        }
        agenteUsuarioId = null
        agentePerfilId = null
        paqueteRepository.deleteAll()
        agenciaRepository.deleteAll()
        hotelRepository.deleteAll()
    }

    private fun samplePaquete(): Paquete {
        val agencia = agenciaService.create(sampleRequest())
        val hotel = hotelRepository.save(Hotel(nombre = "Hotel", pais = "Argentina", foto = "foto"))
        return paqueteRepository.save(Paquete(
            precio = 100f, vueloIdaId = 1L, vueloVueltaId = 2L, hotel = hotel, agencia = agencia,
            destino = "Buenos Aires", origen = "Cordoba",
            fechaInicial = LocalDate.of(2026, 10, 1), fechaFinal = LocalDate.of(2026, 10, 5),
        ))
    }

    @Test
    fun `update preserves package relationship`() {
        val paquete = samplePaquete()
        val id = paquete.agencia.id!!
        agenciaService.update(id, AgenciaRequestDTO("Nueva agencia", "new@example.com"))
        val persisted = paqueteRepository.findById(paquete.id!!).orElseThrow()
        assertEquals(id, persisted.agencia.id)
        assertEquals("new@example.com", persisted.agencia.email)
    }

    @Test
    fun `delete cascades to packages without other references`() {
        val paquete = samplePaquete()
        agenciaService.delete(paquete.agencia.id!!)
        assertEquals(0, agenciaRepository.count())
        assertEquals(0, paqueteRepository.count())
        assertEquals(1, hotelRepository.count())
    }

    @Test
    fun `delete with an agent returns 409 and rolls back package deletion`() {
        val paquete = samplePaquete()
        val usuario = usuarioRepository.save(Usuario(
            nombre = "Agente", apellido = "Test", correo = "agency-agent@example.com",
            direccion = "Direccion", passwordHash = "hash", perfil = Agente(paquete.agencia),
        ))
        agenteUsuarioId = usuario.id
        agentePerfilId = usuario.perfil.id
        val exception = assertThrows(ResponseStatusException::class.java) {
            agenciaService.delete(paquete.agencia.id!!)
        }
        assertEquals(409, exception.statusCode.value())
        assertEquals(1, agenciaRepository.count())
        assertEquals(1, paqueteRepository.count())
    }

    private fun sampleRequest(nombre: String = "Sheraton") = AgenciaRequestDTO(
        nombre = nombre,
        email = "agencia@example.com",
    )

    @Test
    fun `create persists a agencia and returns it with an id`() {
        val created = agenciaService.create(sampleRequest())

        assertEquals("Sheraton", created.nombre)
        assertEquals(1, agenciaRepository.count())
    }

    @Test
    fun `findById returns the persisted agencia`() {
        val created = agenciaService.create(sampleRequest())

        val fetched = agenciaService.findById(created.id!!)

        assertEquals(created.id, fetched.id)
        assertEquals(created.email, fetched.email)
    }

    @Test
    fun `findById throws 404 when the agencia does not exist`() {
        assertThrows(ResponseStatusException::class.java) { agenciaService.findById(999L) }
    }

    @Test
    fun `update overwrites the existing agencia`() {
        val created = agenciaService.create(sampleRequest())

        val updated = agenciaService.update(created.id!!, sampleRequest(nombre = "Hilton"))

        assertEquals("Hilton", updated.nombre)
        assertEquals(1, agenciaRepository.count())
    }

    @Test
    fun `update throws 404 when the agencia does not exist`() {
        assertThrows(ResponseStatusException::class.java) { agenciaService.update(999L, sampleRequest()) }
    }

    @Test
    fun `delete removes the agencia`() {
        val created = agenciaService.create(sampleRequest())

        agenciaService.delete(created.id!!)

        assertEquals(0, agenciaRepository.count())
    }

    @Test
    fun `delete throws 404 when the agencia does not exist`() {
        assertThrows(ResponseStatusException::class.java) { agenciaService.delete(999L) }
    }
}
