package com.aterrizAR.backend.compra

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.auth.RefreshTokenRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.paquete.PaqueteRepository
import com.aterrizAR.backend.usuario.UsuarioRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompraControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var restTemplate: RestTemplate
    @Autowired lateinit var compraRepository: CompraRepository
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var paqueteRepository: PaqueteRepository
    @Autowired lateinit var hotelRepository: HotelRepository
    @Autowired lateinit var agenciaRepository: AgenciaRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired lateinit var jdbc: JdbcTemplate

    private lateinit var mockServer: MockRestServiceServer
    private lateinit var comprador: Usuario
    private lateinit var agencia: Agencia
    private lateinit var hotel: Hotel
    private lateinit var paquete: Paquete

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build()
        comprador = usuario("compra-buyer@example.com", Comprador())
        agencia = agenciaRepository.save(Agencia(nombre = "Viajes Sur", email = "compras-agencia@example.com"))
        hotel = hotelRepository.save(Hotel(nombre = "Hotel Sur", pais = "Argentina", foto = "sur.jpg"))
        paquete = paquete(vueloIdaId = 11, vueloVueltaId = 12)
    }

    @AfterEach
    fun cleanUp() {
        compraRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        usuarioRepository.deleteAll()
        paqueteRepository.deleteAll()
        agenciaRepository.deleteAll()
        hotelRepository.deleteAll()
    }

    private fun usuario(correo: String, perfil: Perfil) = usuarioRepository.save(Usuario(
        nombre = "Test", apellido = "User", correo = correo, direccion = "Quilmes", passwordHash = "unused", perfil = perfil,
    ))

    private fun paquete(vueloIdaId: Long, vueloVueltaId: Long) = paqueteRepository.save(Paquete(
        precio = 1500f, vueloIdaId = vueloIdaId, vueloVueltaId = vueloVueltaId, hotel = hotel, agencia = agencia,
        origen = "Buenos Aires", destino = "Bariloche",
        fechaInicial = LocalDate.now().plusDays(30), fechaFinal = LocalDate.now().plusDays(40),
    ))

    private fun comprar(paqueteId: Long?, operacionId: UUID, actor: Usuario = comprador) = perform(
        post("/api/compras").contentType(MediaType.APPLICATION_JSON)
            .content("""{"paqueteId":$paqueteId,"operacionId":"$operacionId"}"""),
        actor,
    )

    private fun perform(request: MockHttpServletRequestBuilder, actor: Usuario) =
        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtService.createToken(actor)}"))

    private fun expectVenta(operacionId: UUID, vueloIds: List<Long>) =
        mockServer.expect(requestTo("http://flying-service:8081/vuelos/venta"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""{"operacionId":"$operacionId","vueloIds":$vueloIds}""", true))

    private fun ventaJson(operacionId: UUID, vueloIds: List<Long>) =
        """{"operacionId":"$operacionId","vueloIds":$vueloIds,"creadaEn":"2026-10-20T12:00:00Z"}"""

    @Test
    fun `a buyer purchases a package at the current price and its two flights are sold`() {
        val operacionId = UUID.randomUUID()
        expectVenta(operacionId, listOf(11, 12))
            .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(ventaJson(operacionId, listOf(11, 12))))

        comprar(paquete.id, operacionId)
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.operacionId").value(operacionId.toString()))
            .andExpect(jsonPath("$.precio").value(1500.0))
            .andExpect(jsonPath("$.fecha").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.paquete.id").value(paquete.id!!))
            .andExpect(jsonPath("$.paquete.agencia.nombre").value(agencia.nombre))
            .andExpect(jsonPath("$.comprador").doesNotExist())
        mockServer.verify()

        jdbc.update("UPDATE paquete SET precio = ? WHERE id = ?", 2000f, paquete.id)
        val compra = compraRepository.findAll().single()
        assertEquals(1500f, compra.precio)
        assertEquals(operacionId, compra.operacionId)
        assertEquals(agencia.id, compra.agencia.id)
        assertEquals(comprador.perfil.id, compra.comprador.id)
    }

    @Test
    fun `repeating an operation returns the original purchase without selling again`() {
        val operacionId = UUID.randomUUID()
        expectVenta(operacionId, listOf(11, 12))
            .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(ventaJson(operacionId, listOf(11, 12))))
        comprar(paquete.id, operacionId).andExpect(status().isCreated)
        val id = compraRepository.findAll().single().id

        comprar(paquete.id, operacionId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id!!))
        mockServer.verify()
        assertEquals(1, compraRepository.count())
    }

    @Test
    fun `a sale already registered in the flying service is stored when the purchase is retried`() {
        val operacionId = UUID.randomUUID()
        expectVenta(operacionId, listOf(11, 12))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(ventaJson(operacionId, listOf(11, 12))))

        comprar(paquete.id, operacionId).andExpect(status().isCreated)
        assertEquals(1, compraRepository.count())
    }

    @Test
    fun `an operation cannot be reused for another package or by another buyer`() {
        val operacionId = UUID.randomUUID()
        expectVenta(operacionId, listOf(11, 12))
            .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(ventaJson(operacionId, listOf(11, 12))))
        comprar(paquete.id, operacionId).andExpect(status().isCreated)

        val otro = paquete(vueloIdaId = 21, vueloVueltaId = 22)
        comprar(otro.id, operacionId).andExpect(status().isConflict)
        comprar(paquete.id, operacionId, usuario("compra-other@example.com", Comprador())).andExpect(status().isConflict)
        mockServer.verify()
        assertEquals(1, compraRepository.count())
    }

    @Test
    fun `flight rejections and an unavailable flying service do not store a purchase`() {
        val sinLugar = UUID.randomUUID()
        val vueloInexistente = UUID.randomUUID()
        val caido = UUID.randomUUID()
        val integracionRota = UUID.randomUUID()
        expectVenta(sinLugar, listOf(11, 12)).andRespond(withStatus(HttpStatus.CONFLICT))
        expectVenta(vueloInexistente, listOf(11, 12)).andRespond(withResourceNotFound())
        expectVenta(caido, listOf(11, 12)).andRespond(withServerError())
        expectVenta(integracionRota, listOf(11, 12)).andRespond(withStatus(HttpStatus.BAD_REQUEST))

        comprar(paquete.id, sinLugar).andExpect(status().isConflict)
        comprar(paquete.id, vueloInexistente).andExpect(status().isConflict)
        comprar(paquete.id, caido).andExpect(status().isServiceUnavailable)
        // Other client errors are integration failures, not a lack of seats.
        comprar(paquete.id, integracionRota).andExpect(status().isServiceUnavailable)

        mockServer.verify()
        assertEquals(0, compraRepository.count())
    }

    @Test
    fun `missing packages and invalid payloads are rejected without selling flights`() {
        comprar(999999, UUID.randomUUID()).andExpect(status().isNotFound)
        for (id in listOf(0L, -1L, null)) comprar(id, UUID.randomUUID()).andExpect(status().isBadRequest)
        perform(
            post("/api/compras").contentType(MediaType.APPLICATION_JSON).content("""{"paqueteId":${paquete.id}}"""),
            comprador,
        ).andExpect(status().isBadRequest)
        mockServer.verify()
        assertEquals(0, compraRepository.count())
    }

    @Test
    fun `visitors and other roles cannot purchase`() {
        mockMvc.perform(
            post("/api/compras").contentType(MediaType.APPLICATION_JSON)
                .content("""{"paqueteId":${paquete.id},"operacionId":"${UUID.randomUUID()}"}"""),
        ).andExpect(status().isUnauthorized)
        val admin = usuario("compra-admin@example.com", Administrador())
        val agente = usuario("compra-agent@example.com", Agente(agencia = agencia))
        for (actor in listOf(admin, agente)) comprar(paquete.id, UUID.randomUUID(), actor).andExpect(status().isForbidden)
        mockServer.verify()
        assertEquals(0, compraRepository.count())
    }
}
