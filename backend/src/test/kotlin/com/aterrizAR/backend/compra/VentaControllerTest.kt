package com.aterrizAR.backend.compra

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.auth.RefreshTokenRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Compra
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.paquete.PaqueteRepository
import com.aterrizAR.backend.usuario.UsuarioRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VentaControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var compraRepository: CompraRepository
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var paqueteRepository: PaqueteRepository
    @Autowired lateinit var hotelRepository: HotelRepository
    @Autowired lateinit var agenciaRepository: AgenciaRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository

    private lateinit var propia: Agencia
    private lateinit var ajena: Agencia
    private lateinit var agente: Usuario
    private lateinit var cliente: Usuario
    private lateinit var hotel: Hotel

    @BeforeEach
    fun setUp() {
        propia = agenciaRepository.save(Agencia(nombre = "Propia", email = "ventas-propia@example.com"))
        ajena = agenciaRepository.save(Agencia(nombre = "Ajena", email = "ventas-ajena@example.com"))
        hotel = hotelRepository.save(Hotel(nombre = "Hotel Sur", pais = "Argentina", foto = "sur.jpg"))
        agente = usuario("venta-agente@example.com", Agente(propia))
        cliente = usuario("venta-cliente@example.com", Comprador())
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
        nombre = "Ana", apellido = "Gómez", correo = correo, direccion = "Quilmes", passwordHash = "unused", perfil = perfil,
    ))

    private fun venta(agencia: Agencia, fecha: LocalDate, precio: Float) {
        val paquete = paqueteRepository.save(Paquete(
            precio = precio, vueloIdaId = precio.toLong(), vueloVueltaId = precio.toLong() + 1, hotel = hotel,
            agencia = agencia, origen = "Buenos Aires", destino = "Bariloche",
            fechaInicial = LocalDate.now().plusDays(30), fechaFinal = LocalDate.now().plusDays(40),
        ))
        compraRepository.save(Compra(
            operacionId = UUID.randomUUID(), precio = precio, fecha = fecha, paquete = paquete, agencia = agencia,
            comprador = cliente.perfil as Comprador,
        ))
    }

    private fun listar(actor: Usuario?) = mockMvc.perform(
        get("/api/ventas").also { request ->
            actor?.let { request.header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtService.createToken(it)}") }
        },
    )

    @Test
    fun `an agent lists only the sales of its agency with the client, newest first`() {
        venta(propia, LocalDate.now().minusDays(3), 1000f)
        venta(propia, LocalDate.now().minusDays(1), 2000f)
        venta(ajena, LocalDate.now(), 3000f)

        listar(agente)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].precio").value(2000.0))
            .andExpect(jsonPath("$[1].precio").value(1000.0))
            .andExpect(jsonPath("$[0].destino").value("Bariloche"))
            .andExpect(jsonPath("$[0].hotel").value("Hotel Sur"))
            .andExpect(jsonPath("$[0].cliente.correo").value("venta-cliente@example.com"))
            .andExpect(jsonPath("$[0].cliente.nombre").value("Ana"))
            .andExpect(jsonPath("$[0].cliente.passwordHash").doesNotExist())
    }

    @Test
    fun `an agency without sales gets an empty list`() {
        venta(ajena, LocalDate.now(), 3000f)

        listar(agente).andExpect(status().isOk).andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `only agents can consult sales`() {
        listar(null).andExpect(status().isUnauthorized)
        listar(cliente).andExpect(status().isForbidden)
        listar(usuario("venta-admin@example.com", com.aterrizAR.backend.model.Administrador())).andExpect(status().isForbidden)
    }
}
