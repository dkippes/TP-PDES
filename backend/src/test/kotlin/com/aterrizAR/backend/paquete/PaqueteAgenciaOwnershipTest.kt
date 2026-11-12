package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.auth.RefreshTokenRepository
import com.aterrizAR.backend.compra.CompraRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Compra
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.usuario.UsuarioRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaqueteAgenciaOwnershipTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var restTemplate: RestTemplate
    @Autowired lateinit var paqueteRepository: PaqueteRepository
    @Autowired lateinit var compraRepository: CompraRepository
    @Autowired lateinit var hotelRepository: HotelRepository
    @Autowired lateinit var agenciaRepository: AgenciaRepository
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository

    private lateinit var mockServer: MockRestServiceServer
    private lateinit var propia: Agencia
    private lateinit var ajena: Agencia
    private lateinit var hotel: Hotel
    private lateinit var agente: Usuario

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build()
        propia = agenciaRepository.save(Agencia(nombre = "Propia", email = "own-propia@example.com"))
        ajena = agenciaRepository.save(Agencia(nombre = "Ajena", email = "own-ajena@example.com"))
        hotel = hotelRepository.save(Hotel(nombre = "Hotel Sur", pais = "Argentina", foto = "sur.jpg"))
        agente = usuario("own-agente@example.com", Agente(propia))
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

    private fun paquete(agencia: Agencia) = paqueteRepository.save(Paquete(
        precio = 1500f, vueloIdaId = 1, vueloVueltaId = 2, hotel = hotel, agencia = agencia,
        origen = "Buenos Aires", destino = "Bariloche",
        fechaInicial = LocalDate.of(2026, 12, 1), fechaFinal = LocalDate.of(2026, 12, 10),
    ))

    private fun json(agencia: Agencia) = """
        {"precio":1500.0,"vueloIdaId":1,"vueloVueltaId":2,"hotelId":${hotel.id},"agenciaId":${agencia.id},
         "destino":"Bariloche","origen":"Buenos Aires","fechaInicial":"2026-12-01","fechaFinal":"2026-12-10"}
    """.trimIndent()

    private fun comoAgente(request: MockHttpServletRequestBuilder) =
        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtService.createToken(agente)}"))

    private fun expectDisponibilidad() {
        for ((id, disponibilidad) in listOf(1L to 5, 2L to 5)) {
            mockServer.expect(requestTo("http://flying-service:8081/vuelos/$id"))
                .andRespond(withSuccess("""{"id":$id,"disponibilidad":$disponibilidad}""", MediaType.APPLICATION_JSON))
        }
        mockServer.expectDisponibilidades(1L to 5, 2L to 5)
    }

    @Test
    fun `an agent creates packages for its own agency only`() {
        expectDisponibilidad()
        comoAgente(post("/api/paquetes").contentType(MediaType.APPLICATION_JSON).content(json(propia)))
            .andExpect(status().isCreated)

        comoAgente(post("/api/paquetes").contentType(MediaType.APPLICATION_JSON).content(json(ajena)))
            .andExpect(status().isForbidden)
        assertEquals(1, paqueteRepository.count())
    }

    @Test
    fun `an agent cannot update, move or delete packages of another agency`() {
        val ajeno = paquete(ajena)
        comoAgente(put("/api/paquetes/${ajeno.id}").contentType(MediaType.APPLICATION_JSON).content(json(ajena)))
            .andExpect(status().isForbidden)
        comoAgente(delete("/api/paquetes/${ajeno.id}")).andExpect(status().isForbidden)

        val propio = paquete(propia)
        comoAgente(put("/api/paquetes/${propio.id}").contentType(MediaType.APPLICATION_JSON).content(json(ajena)))
            .andExpect(status().isForbidden)
        assertEquals(2, paqueteRepository.count())
    }

    @Test
    fun `an agent deletes its own package unless it has purchases`() {
        val vendido = paquete(propia)
        compraRepository.save(Compra(
            operacionId = UUID.randomUUID(), precio = 1500f, fecha = LocalDate.now(), paquete = vendido, agencia = propia,
            comprador = usuario("own-cliente@example.com", Comprador()).perfil as Comprador,
        ))
        comoAgente(delete("/api/paquetes/${vendido.id}")).andExpect(status().isConflict)

        val libre = paqueteRepository.save(Paquete(
            precio = 900f, vueloIdaId = 7, vueloVueltaId = 8, hotel = hotel, agencia = propia, origen = "Rosario",
            destino = "Salta", fechaInicial = LocalDate.of(2026, 12, 1), fechaFinal = LocalDate.of(2026, 12, 10),
        ))
        comoAgente(delete("/api/paquetes/${libre.id}")).andExpect(status().isNoContent)
        assertEquals(1, paqueteRepository.count())
    }

    private fun comprar(paquete: Paquete) = compraRepository.save(Compra(
        operacionId = UUID.randomUUID(), precio = paquete.precio, fecha = LocalDate.now(), paquete = paquete, agencia = propia,
        comprador = usuario("own-comprador-${UUID.randomUUID()}@example.com", Comprador()).perfil as Comprador,
    ))

    @Test
    fun `an agent edits a package nobody bought`() {
        val libre = paquete(propia)
        expectDisponibilidad()

        comoAgente(put("/api/paquetes/${libre.id}").contentType(MediaType.APPLICATION_JSON).content(json(propia).replace("1500.0", "1800.0")))
            .andExpect(status().isOk)
        assertEquals(1800f, paqueteRepository.findById(libre.id!!).get().precio)
    }

    @Test
    fun `a package with purchases cannot be edited and keeps its data`() {
        val vendido = paquete(propia)
        comprar(vendido)

        comoAgente(put("/api/paquetes/${vendido.id}").contentType(MediaType.APPLICATION_JSON).content(json(propia).replace("1500.0", "1800.0")))
            .andExpect(status().isConflict)
        assertEquals(1500f, paqueteRepository.findById(vendido.id!!).get().precio)
    }
}
