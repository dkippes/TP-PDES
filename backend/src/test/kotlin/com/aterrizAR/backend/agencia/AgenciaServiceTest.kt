package com.aterrizAR.backend.agencia

import com.aterrizAR.backend.auth.UsuarioRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import com.aterrizAR.backend.model.Roles
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.paquete.PaqueteRepository
import com.aterrizAR.backend.security.UsuarioAutenticado
import java.time.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.server.ResponseStatusException

// Sin @Transactional: los tests de borrado necesitan que el servicio confirme o revierta su propia transacción.
@SpringBootTest
@ActiveProfiles("test")
class AgenciaServiceTest {

    @Autowired
    lateinit var agenciaService: AgenciaService

    @Autowired
    lateinit var agenciaRepository: AgenciaRepository

    @Autowired
    lateinit var paqueteRepository: PaqueteRepository

    @Autowired
    lateinit var hotelRepository: HotelRepository

    @Autowired
    lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private val administrador = UsuarioAutenticado(id = 1L, rol = Roles.ADMINISTRADOR)

    private var agenteUsuarioId: Long? = null
    private var agentePerfilId: Long? = null

    @AfterEach
    fun cleanUp() {
        agenteUsuarioId?.let { usuarioRepository.deleteById(it) }
        // Usuario solo propaga PERSIST al perfil y no hay repositorio de perfiles: se borra el Agente a mano.
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

    private fun sampleRequest(
        nombre: String = "Viajes Sur",
        email: String = "ventas@viajessur.com",
    ) = AgenciaRequestDTO(nombre = nombre, email = email)

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
    fun `create persists an agencia and returns it with an id`() {
        val created = agenciaService.create(sampleRequest())

        assertEquals("Viajes Sur", created.nombre)
        assertEquals(1, agenciaRepository.count())
    }

    @Test
    fun `create trims the nombre and normalizes the email`() {
        val created = agenciaService.create(sampleRequest(nombre = "  Viajes Sur  ", email = " Ventas@ViajesSur.com "))

        assertEquals("Viajes Sur", created.nombre)
        assertEquals("ventas@viajessur.com", created.email)
    }

    @Test
    fun `create rejects an email that differs only in case with 409`() {
        agenciaService.create(sampleRequest())

        val exception = assertThrows(ResponseStatusException::class.java) {
            agenciaService.create(sampleRequest(nombre = "Otra", email = "VENTAS@viajessur.com"))
        }

        assertEquals(409, exception.statusCode.value())
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

        val updated = agenciaService.update(created.id!!, sampleRequest(nombre = "Viajes Norte"), administrador)

        assertEquals("Viajes Norte", updated.nombre)
        assertEquals(1, agenciaRepository.count())
    }

    @Test
    fun `update keeps its own email and rejects another agency email with 409`() {
        val first = agenciaService.create(sampleRequest())
        val second = agenciaService.create(sampleRequest(nombre = "Otra", email = "otra@example.com"))

        agenciaService.update(first.id!!, sampleRequest(nombre = "Renombrada"), administrador)
        val exception = assertThrows(ResponseStatusException::class.java) {
            agenciaService.update(second.id!!, sampleRequest(email = "Ventas@ViajesSur.com"), administrador)
        }

        assertEquals(409, exception.statusCode.value())
        assertEquals("otra@example.com", agenciaRepository.findById(second.id!!).orElseThrow().email)
    }

    @Test
    fun `update throws 404 when the agencia does not exist`() {
        assertThrows(ResponseStatusException::class.java) {
            agenciaService.update(999L, sampleRequest(), administrador)
        }
    }

    @Test
    fun `update preserves package relationship`() {
        val paquete = samplePaquete()
        val id = paquete.agencia.id!!

        agenciaService.update(id, sampleRequest(nombre = "Nueva agencia", email = "new@example.com"), administrador)

        val persisted = paqueteRepository.findById(paquete.id!!).orElseThrow()
        assertEquals(id, persisted.agencia.id)
        assertEquals("new@example.com", persisted.agencia.email)
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
}
