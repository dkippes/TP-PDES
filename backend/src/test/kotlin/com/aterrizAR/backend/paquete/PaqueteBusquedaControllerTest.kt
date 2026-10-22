package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaqueteBusquedaControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var paqueteRepository: PaqueteRepository

    @Autowired
    lateinit var hotelRepository: HotelRepository

    @Autowired
    lateinit var agenciaRepository: AgenciaRepository

    @Autowired
    lateinit var restTemplate: RestTemplate

    private lateinit var mockServer: MockRestServiceServer
    private lateinit var agencia: Agencia

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build()
        val hotel = hotelRepository.save(Hotel(nombre = "Sheraton", pais = "Argentina", foto = "sheraton.jpg"))
        agencia = agenciaRepository.save(Agencia(nombre = "Agencia Sur", email = "busqueda-ctrl@test.com"))
        listOf(
            Triple("Bariloche", 1000f, "2026-12-01"),
            Triple("Madrid", 3000f, "2027-01-10"),
            Triple("Cordoba", 500f, "2026-11-05"),
        ).forEachIndexed { index, (destino, precio, fecha) ->
            paqueteRepository.save(
                Paquete(
                    precio = precio,
                    vueloIdaId = index * 2L + 1,
                    vueloVueltaId = index * 2L + 2,
                    hotel = hotel,
                    agencia = agencia,
                    destino = destino,
                    origen = "Buenos Aires",
                    fechaInicial = LocalDate.parse(fecha),
                    fechaFinal = LocalDate.parse(fecha).plusDays(9),
                ),
            )
        }
    }

    @AfterEach
    fun cleanUp() {
        paqueteRepository.deleteAll()
        hotelRepository.deleteAll()
        agenciaRepository.deleteAll()
    }

    @Test
    fun `GET buscar is public and returns the page shape with defaults`() {
        mockServer.expectDisponibilidades(1L to 10, 2L to 4, 3L to 6, 4L to 6, 5L to 0, 6L to 20)

        mockMvc.perform(get("/api/paquetes/buscar"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.contenido.length()").value(3))
            .andExpect(jsonPath("$.contenido[0].destino").value("Cordoba"))
            .andExpect(jsonPath("$.contenido[0].hotel.nombre").value("Sheraton"))
            .andExpect(jsonPath("$.contenido[0].disponibilidad").value(0))
            .andExpect(jsonPath("$.contenido[1].destino").value("Bariloche"))
            .andExpect(jsonPath("$.contenido[1].disponibilidad").value(4))
            .andExpect(jsonPath("$.contenido[2].disponibilidad").value(6))
            .andExpect(jsonPath("$.pagina").value(0))
            .andExpect(jsonPath("$.tamanio").value(12))
            .andExpect(jsonPath("$.totalElementos").value(3))
            .andExpect(jsonPath("$.totalPaginas").value(1))
    }

    @Test
    fun `GET buscar binds filters, order and pagination`() {
        mockServer.expectDisponibilidades(1L to 10, 2L to 4)

        mockMvc.perform(
            get("/api/paquetes/buscar")
                .param("q", "sheraton")
                .param("precioMin", "600")
                .param("precioMax", "3000")
                .param("fechaDesde", "2026-12-01")
                .param("fechaHasta", "2027-12-31")
                .param("agenciaId", agencia.id.toString())
                .param("orden", "PRECIO_DESC")
                .param("pagina", "1")
                .param("tamanio", "1"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.contenido.length()").value(1))
            .andExpect(jsonPath("$.contenido[0].destino").value("Bariloche"))
            .andExpect(jsonPath("$.contenido[0].disponibilidad").value(4))
            .andExpect(jsonPath("$.pagina").value(1))
            .andExpect(jsonPath("$.tamanio").value(1))
            .andExpect(jsonPath("$.totalElementos").value(2))
            .andExpect(jsonPath("$.totalPaginas").value(2))
    }

    @Test
    fun `GET buscar rejects invalid parameters with 400`() {
        listOf(
            "precioMin" to "-1",
            "precioMax" to "-1",
            "agenciaId" to "0",
            "tamanio" to "0",
            "tamanio" to "51",
            "pagina" to "-1",
            "orden" to "INVALIDO",
            "fechaDesde" to "no-es-fecha",
        ).forEach { (name, value) ->
            mockMvc.perform(get("/api/paquetes/buscar").param(name, value))
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `GET buscar rejects inverted ranges with 400`() {
        mockMvc.perform(get("/api/paquetes/buscar").param("precioMin", "500").param("precioMax", "100"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(
            get("/api/paquetes/buscar").param("fechaDesde", "2027-01-01").param("fechaHasta", "2026-01-01"),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET buscar reports null availability when flying-service fails`() {
        mockServer.expectDisponibilidadesFallidas(1, 2, 3, 4, 5, 6)

        mockMvc.perform(get("/api/paquetes/buscar"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.contenido.length()").value(3))
            .andExpect(jsonPath("$.contenido[0].disponibilidad").hasJsonPath())
            .andExpect(jsonPath("$.contenido[0].disponibilidad").value(null as Any?))
    }

    @Test
    fun `GET paquetes still returns a plain array`() {
        mockServer.expectDisponibilidades(1L to 10, 2L to 4, 3L to 6, 4L to 6, 5L to 0, 6L to 20)

        mockMvc.perform(get("/api/paquetes"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
    }
}
