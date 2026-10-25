package com.aterrizAR.backend.valoracion

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.auth.RefreshTokenRepository
import com.aterrizAR.backend.compra.CompraRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.*
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
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValoracionControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var valoracionRepository: ValoracionRepository
    @Autowired lateinit var compraRepository: CompraRepository
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var paqueteRepository: PaqueteRepository
    @Autowired lateinit var hotelRepository: HotelRepository
    @Autowired lateinit var agenciaRepository: AgenciaRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var jdbc: JdbcTemplate

    private lateinit var comprador: Usuario
    private lateinit var otro: Usuario
    private lateinit var admin: Usuario
    private lateinit var agente: Usuario
    private lateinit var paquete: Paquete
    private lateinit var compra: Compra

    @BeforeEach
    fun setUp() {
        comprador = usuario("valoracion-buyer@example.com", Comprador())
        otro = usuario("valoracion-other@example.com", Comprador())
        admin = usuario("valoracion-admin@example.com", Administrador())
        val agencia = agenciaRepository.save(Agencia(nombre = "Viajes Sur", email = "valoracion-agencia@example.com"))
        agente = usuario("valoracion-agente@example.com", Agente(agencia))
        val hotel = hotelRepository.save(Hotel(nombre = "Hotel Sur", pais = "Argentina", foto = ""))
        paquete = paqueteRepository.save(Paquete(
            precio = 1500f, vueloIdaId = 1, vueloVueltaId = 2, hotel = hotel, agencia = agencia,
            origen = "Buenos Aires", destino = "Bariloche",
            fechaInicial = LocalDate.now().plusDays(30), fechaFinal = LocalDate.now().plusDays(40),
        ))
        compra = comprar(comprador)
    }

    @AfterEach
    fun cleanUp() {
        valoracionRepository.deleteAll()
        compraRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        usuarioRepository.deleteAll()
        paqueteRepository.deleteAll()
        agenciaRepository.deleteAll()
        hotelRepository.deleteAll()
    }

    private fun usuario(correo: String, perfil: Perfil) = usuarioRepository.save(Usuario(
        nombre = "Nombre privado", apellido = "Apellido privado", correo = correo, direccion = "Quilmes", passwordHash = "unused", perfil = perfil,
    ))

    private fun comprar(actor: Usuario) = compraRepository.saveAndFlush(Compra(
        operacionId = UUID.randomUUID(), precio = 1500f, fecha = LocalDate.now(), paquete = paquete,
        agencia = paquete.agencia, comprador = actor.perfil as Comprador,
    ))

    private fun perform(request: MockHttpServletRequestBuilder, actor: Usuario = comprador) =
        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtService.createToken(actor)}"))

    private fun guardar(body: String, compraId: Long = compra.id!!, actor: Usuario = comprador) =
        perform(put("/api/compras/$compraId/valoracion").contentType(MediaType.APPLICATION_JSON).content(body), actor)

    @Test
    fun `el dueno puede valorar editar y eliminar sin modificar su compra`() {
        guardar("""{"puntaje":0}""").andExpect(status().isOk)
            .andExpect(jsonPath("$.compraId").value(compra.id!!))
            .andExpect(jsonPath("$.valoracion.puntaje").value(0))
            .andExpect(jsonPath("$.valoracion.comentario").value(null as Any?))
        val inicial = valoracionRepository.findByCompra_Id(compra.id!!)!!
        guardar("""{"puntaje":10,"comentario":"  Muy bueno  "}""").andExpect(status().isOk)
            .andExpect(jsonPath("$.valoracion.id").value(inicial.id!!))
            .andExpect(jsonPath("$.valoracion.comentario").value("Muy bueno"))
        assertEquals(inicial.fecha, valoracionRepository.findByCompra_Id(compra.id!!)!!.fecha)
        assertEquals(1, valoracionRepository.count())
        guardar("""{"puntaje":8,"comentario":"  "}""").andExpect(status().isOk)
            .andExpect(jsonPath("$.valoracion.comentario").value(null as Any?))
        repeat(2) { perform(delete("/api/compras/${compra.id}/valoracion")).andExpect(status().isNoContent) }
        assertEquals(0, valoracionRepository.count())
        assertEquals(1500f, compraRepository.findById(compra.id!!).get().precio)
        guardar("""{"puntaje":5,"comentario":null}""").andExpect(status().isOk)
    }

    @Test
    fun `cada compra permite una valoracion y el promedio cuenta tambien los ceros`() {
        val segunda = comprar(comprador)
        guardar("""{"puntaje":0}""")
        guardar("""{"puntaje":10,"comentario":"Excelente"}""", segunda.id!!)
        jdbc.update("UPDATE valoracion SET fecha = CURRENT_TIMESTAMP - INTERVAL '1' DAY WHERE compra_id = ?", compra.id)
        perform(get("/api/valoraciones/mias")).andExpect(status().isOk).andExpect(jsonPath("$.length()").value(2))
        mockMvc.perform(get("/api/paquetes/${paquete.id}/valoraciones"))
            .andExpect(status().isOk).andExpect(jsonPath("$.promedio").value(5.0))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.valoraciones[0].puntaje").value(10))
        guardar("""{"puntaje":6}""", segunda.id!!)
        mockMvc.perform(get("/api/paquetes/${paquete.id}/valoraciones")).andExpect(jsonPath("$.promedio").value(3.0))
        perform(delete("/api/compras/${segunda.id}/valoracion"))
        mockMvc.perform(get("/api/paquetes/${paquete.id}/valoraciones")).andExpect(jsonPath("$.promedio").value(0.0))
    }

    @Test
    fun `las lecturas publicas son anonimas para visitantes y todos los roles`() {
        guardar("""{"puntaje":7,"comentario":"Comentario publico"}""")
        val path = "/api/paquetes/${paquete.id}/valoraciones"
        val respuestas = listOf(mockMvc.perform(get(path))) + listOf(comprador, otro, admin, agente).map { perform(get(path), it) }
        respuestas.forEach { respuesta ->
            val root = mapper.readTree(respuesta.andExpect(status().isOk).andReturn().response.contentAsString)
            assertEquals(setOf("promedio", "total", "valoraciones"), root.propertyNames().toSet())
            assertEquals(setOf("id", "puntaje", "comentario", "fecha"), root["valoraciones"][0].propertyNames().toSet())
        }
        perform(get("/api/valoraciones/mias"), otro).andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `un comprador sin esa compra no puede crear editar ni eliminar su valoracion`() {
        guardar("""{"puntaje":7}""", actor = otro).andExpect(status().isNotFound)
        guardar("""{"puntaje":9}""").andExpect(status().isOk)
        guardar("""{"puntaje":0}""", actor = otro).andExpect(status().isNotFound)
        perform(delete("/api/compras/${compra.id}/valoracion"), otro).andExpect(status().isNotFound)
        assertEquals(9, valoracionRepository.findByCompra_Id(compra.id!!)!!.puntaje)
    }

    @Test
    fun `visitantes administradores y agentes no pueden gestionar valoraciones`() {
        val requests = listOf(
            { get("/api/valoraciones/mias") },
            { put("/api/compras/${compra.id}/valoracion").contentType(MediaType.APPLICATION_JSON).content("""{"puntaje":8}""") },
            { delete("/api/compras/${compra.id}/valoracion") },
        )
        requests.forEach { request ->
            mockMvc.perform(request()).andExpect(status().isUnauthorized)
            listOf(admin, agente).forEach { perform(request(), it).andExpect(status().isForbidden) }
        }
    }

    @Test
    fun `entradas invalidas no cambian la compra ni la valoracion previa`() {
        guardar("""{"puntaje":8}""")
        listOf("{}", """{"puntaje":null}""", """{"puntaje":-1}""", """{"puntaje":11}""",
            """{"puntaje":8,"comentario":"${"a".repeat(256)}"}""", """{"puntaje":1.5}"""
        ).forEach { guardar(it).andExpect(status().isBadRequest) }
        assertEquals(8, valoracionRepository.findByCompra_Id(compra.id!!)!!.puntaje)
        assertEquals(1, compraRepository.count())
        guardar("""{"puntaje":8,"comentario":"${"a".repeat(255)}"}""").andExpect(status().isOk)
    }

    @Test
    fun `paquetes sin valoraciones e identificadores invalidos tienen respuestas explicitas`() {
        mockMvc.perform(get("/api/paquetes/${paquete.id}/valoraciones")).andExpect(status().isOk)
            .andExpect(jsonPath("$.promedio").value(null as Any?)).andExpect(jsonPath("$.total").value(0))
        mockMvc.perform(get("/api/paquetes/999999/valoraciones")).andExpect(status().isNotFound)
        mockMvc.perform(get("/api/paquetes/0/valoraciones")).andExpect(status().isBadRequest)
        guardar("""{"puntaje":8}""", 0).andExpect(status().isBadRequest)
        guardar("""{"puntaje":8}""", 999999).andExpect(status().isNotFound)
        perform(delete("/api/compras/-1/valoracion")).andExpect(status().isBadRequest)
    }

    @Test
    fun `el rol persistido se verifica aunque el token siga vigente`() {
        // El token declara COMPRADOR, pero la cuenta guardada sigue teniendo perfil ADMINISTRADOR.
        val token = jwtService.createToken(admin.apply { perfil = Comprador() })
        mockMvc.perform(put("/api/compras/${compra.id}/valoracion").header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON).content("""{"puntaje":8}""")).andExpect(status().isForbidden)
    }

    @Test
    fun `reintentos concurrentes no crean mas de una valoracion por compra`() {
        val token = jwtService.createToken(comprador)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val resultados = (1..2).map { pool.submit<Int> {
                start.await(5, TimeUnit.SECONDS)
                mockMvc.perform(put("/api/compras/${compra.id}/valoracion")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token").contentType(MediaType.APPLICATION_JSON)
                    .content("""{"puntaje":8}""")).andReturn().response.status
            } }
            start.countDown()
            resultados.forEach { assertEquals(200, it.get(15, TimeUnit.SECONDS)) }
            assertEquals(1, valoracionRepository.count())
        } finally { pool.shutdownNow() }
    }
}
