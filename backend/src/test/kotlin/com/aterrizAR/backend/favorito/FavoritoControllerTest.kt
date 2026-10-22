package com.aterrizAR.backend.favorito

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
import com.aterrizAR.backend.paquete.expectDisponibilidades
import com.aterrizAR.backend.paquete.expectDisponibilidadesFallidas
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
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoritoControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var favoritoRepository: FavoritoRepository
    @Autowired lateinit var paqueteRepository: PaqueteRepository
    @Autowired lateinit var hotelRepository: HotelRepository
    @Autowired lateinit var agenciaRepository: AgenciaRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var restTemplate: RestTemplate

    private lateinit var mockServer: MockRestServiceServer

    private lateinit var comprador: Usuario
    private lateinit var otroComprador: Usuario
    private lateinit var admin: Usuario
    private lateinit var agencia: Agencia
    private lateinit var hotel: Hotel
    private lateinit var paquete: Paquete

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build()
        comprador = usuario("favorito-buyer@example.com", Comprador())
        otroComprador = usuario("favorito-other@example.com", Comprador())
        admin = usuario("favorito-admin@example.com", Administrador())
        agencia = agenciaRepository.save(Agencia(nombre = "Viajes Sur", email = "favoritos-agencia@example.com"))
        hotel = hotelRepository.save(Hotel(nombre = "Hotel Sur", pais = "Argentina", foto = "sur.jpg"))
        paquete = paquete("Bariloche")
    }

    @AfterEach
    fun cleanUp() {
        favoritoRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        usuarioRepository.deleteAll()
        paqueteRepository.deleteAll()
        agenciaRepository.deleteAll()
        hotelRepository.deleteAll()
    }

    private fun usuario(correo: String, perfil: Perfil) = usuarioRepository.save(Usuario(
        nombre = "Test", apellido = "User", correo = correo, direccion = "Quilmes", passwordHash = "unused", perfil = perfil,
    ))

    private fun paquete(destino: String) = paqueteRepository.save(Paquete(
        precio = 1500f, vueloIdaId = 1, vueloVueltaId = 2, hotel = hotel, agencia = agencia,
        origen = "Buenos Aires", destino = destino,
        fechaInicial = LocalDate.now().plusDays(30), fechaFinal = LocalDate.now().plusDays(40),
    ))

    private fun perform(request: MockHttpServletRequestBuilder, actor: Usuario = comprador) =
        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtService.createToken(actor)}"))

    @Test
    fun `favorites persist without duplicates and only belong to the authenticated buyer`() {
        perform(get("/api/favoritos")).andExpect(status().isOk).andExpect(jsonPath("$.length()").value(0))
        repeat(2) { perform(put("/api/favoritos/${paquete.id}")).andExpect(status().isNoContent) }
        assertEquals(1, favoritoRepository.count())

        mockServer.expectDisponibilidades(1L to 9, 2L to 3)
        perform(get("/api/favoritos"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].paquete.disponibilidad").value(3))
            .andExpect(jsonPath("$[0].paquete.id").value(paquete.id!!))
            .andExpect(jsonPath("$[0].paquete.hotel.nombre").value(hotel.nombre))
            // Same package shape as the catalog, which the shared package card renders.
            .andExpect(jsonPath("$[0].paquete.hotel.pais").value(hotel.pais))
            .andExpect(jsonPath("$[0].paquete.agencia.nombre").value(paquete.agencia.nombre))
            .andExpect(jsonPath("$[0].paquete.fechaInicial").value(paquete.fechaInicial.toString()))
            .andExpect(jsonPath("$[0].paquete.fechaFinal").value(paquete.fechaFinal.toString()))
            .andExpect(jsonPath("$[0].paquete.precio").value(paquete.precio.toDouble()))
            .andExpect(jsonPath("$[0].agregadoEn").exists())
            .andExpect(jsonPath("$[0].comprador").doesNotExist())
        perform(get("/api/favoritos"), otroComprador).andExpect(jsonPath("$.length()").value(0))
        perform(delete("/api/favoritos/${paquete.id}"), otroComprador).andExpect(status().isNoContent)
        assertEquals(1, favoritoRepository.count())
        repeat(2) { perform(delete("/api/favoritos/${paquete.id}")).andExpect(status().isNoContent) }
        assertEquals(0, favoritoRepository.count())
    }

    @Test
    fun `favorites use current package data and newest saved package comes first`() {
        val segundo = paquete("Madrid")
        perform(put("/api/favoritos/${paquete.id}")).andExpect(status().isNoContent)
        perform(put("/api/favoritos/${segundo.id}")).andExpect(status().isNoContent)
        perform(put("/api/favoritos/${paquete.id}")).andExpect(status().isNoContent)
        jdbc.update("UPDATE paquete SET destino = ? WHERE id = ?", "Córdoba", paquete.id)

        mockServer.expectDisponibilidades(1L to 9, 2L to 3)
        perform(get("/api/favoritos"))
            .andExpect(jsonPath("$[0].paquete.id").value(segundo.id!!))
            .andExpect(jsonPath("$[1].paquete.destino").value("Córdoba"))
    }

    @Test
    fun `favorites report null availability when flying-service fails`() {
        perform(put("/api/favoritos/${paquete.id}")).andExpect(status().isNoContent)

        mockServer.expectDisponibilidadesFallidas(1, 2)
        perform(get("/api/favoritos"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].paquete.id").value(paquete.id!!))
            .andExpect(jsonPath("$[0].paquete.disponibilidad").hasJsonPath())
            .andExpect(jsonPath("$[0].paquete.disponibilidad").value(null as Any?))
    }

    @Test
    fun `visitors and other roles cannot access any favorites operation`() {
        val agente = usuario("favorito-agent@example.com", Agente(agencia = agencia))
        val requests = listOf(
            { get("/api/favoritos") }, { put("/api/favoritos/${paquete.id}") }, { delete("/api/favoritos/${paquete.id}") },
        )
        for (request in requests) {
            mockMvc.perform(request()).andExpect(status().isUnauthorized)
            for (actor in listOf(admin, agente)) perform(request(), actor).andExpect(status().isForbidden)
        }
        assertEquals(0, favoritoRepository.count())
    }

    @Test
    fun `invalid identifiers and missing packages do not create favorites`() {
        perform(put("/api/favoritos/999999")).andExpect(status().isNotFound)
        for (id in listOf(0, -1)) {
            perform(put("/api/favoritos/$id")).andExpect(status().isBadRequest)
            perform(delete("/api/favoritos/$id")).andExpect(status().isBadRequest)
        }
        perform(delete("/api/favoritos/999999")).andExpect(status().isNoContent)
        assertEquals(0, favoritoRepository.count())
    }

    @Test
    fun `deleting a package clears all its favorites and preserves others`() {
        val segundo = paquete("Madrid")
        perform(put("/api/favoritos/${paquete.id}")).andExpect(status().isNoContent)
        perform(put("/api/favoritos/${paquete.id}"), otroComprador).andExpect(status().isNoContent)
        perform(put("/api/favoritos/${segundo.id}")).andExpect(status().isNoContent)
        perform(delete("/api/paquetes/${paquete.id}"), admin).andExpect(status().isNoContent)

        assertEquals(1, favoritoRepository.count())
        mockServer.expectDisponibilidades(1L to 9, 2L to 3)
        perform(get("/api/favoritos")).andExpect(jsonPath("$[0].paquete.id").value(segundo.id!!))
    }

    @Test
    fun `deleting an agency clears favorites of its deleted packages`() {
        perform(put("/api/favoritos/${paquete.id}")).andExpect(status().isNoContent)
        perform(delete("/api/agencias/${agencia.id}"), admin).andExpect(status().isNoContent)
        assertEquals(0, favoritoRepository.count())
        assertEquals(0, paqueteRepository.count())
    }

    @Test
    fun `deleting a buyer clears only that buyer favorites`() {
        perform(put("/api/favoritos/${paquete.id}")).andExpect(status().isNoContent)
        perform(put("/api/favoritos/${paquete.id}"), otroComprador).andExpect(status().isNoContent)
        perform(delete("/api/usuarios/${comprador.id}"), admin).andExpect(status().isNoContent)
        assertEquals(1, favoritoRepository.count())
        assertEquals(1, paqueteRepository.count())
        perform(get("/api/favoritos")).andExpect(status().isNotFound)
        perform(put("/api/favoritos/${paquete.id}")).andExpect(status().isNotFound)
        perform(delete("/api/favoritos/${paquete.id}")).andExpect(status().isNotFound)
    }

    @Test
    fun `changing buyer role clears favorites and a stale buyer token loses access`() {
        perform(put("/api/favoritos/${paquete.id}")).andExpect(status().isNoContent)
        val body = """{"nombre":"Test","apellido":"User","correo":"${comprador.correo}","direccion":"Quilmes","rol":"ADMINISTRADOR"}"""
        perform(put("/api/usuarios/${comprador.id}").contentType(MediaType.APPLICATION_JSON).content(body), admin)
            .andExpect(status().isOk)
        assertEquals(0, favoritoRepository.count())
        perform(get("/api/favoritos")).andExpect(status().isForbidden)
        perform(put("/api/favoritos/${paquete.id}")).andExpect(status().isForbidden)
        perform(delete("/api/favoritos/${paquete.id}")).andExpect(status().isForbidden)
    }

    @Test
    fun `simultaneous saves are idempotent`() {
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        Executors.newFixedThreadPool(2).use { executor ->
            val responses = (1..2).map {
                executor.submit<Int> {
                    ready.countDown()
                    check(start.await(10, TimeUnit.SECONDS))
                    perform(put("/api/favoritos/${paquete.id}")).andReturn().response.status
                }
            }
            check(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            responses.forEach { assertEquals(204, it.get(10, TimeUnit.SECONDS)) }
        }
        assertEquals(1, favoritoRepository.count())
    }
}
