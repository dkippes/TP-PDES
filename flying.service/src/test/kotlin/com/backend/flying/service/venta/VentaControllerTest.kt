package com.backend.flying.service.venta

import com.backend.flying.service.vuelo.Vuelo
import com.backend.flying.service.vuelo.VueloRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VentaControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var ventaRepository: VentaRepository

    @Autowired
    lateinit var vueloRepository: VueloRepository

    @AfterEach
    fun cleanUp() {
        ventaRepository.deleteAll()
        vueloRepository.deleteAll()
    }

    private fun vuelo(disponibilidad: Int) = vueloRepository.save(
        Vuelo(
            aerolinea = "Aerolineas Argentinas",
            fecha = LocalDate.of(2026, 12, 1),
            hora = LocalTime.of(10, 0),
            origen = "EZE",
            destino = "BRC",
            capacidad = 180,
            disponibilidad = disponibilidad,
        ),
    ).id!!

    private fun vender(body: String) =
        mockMvc.perform(post("/vuelos/venta").contentType(MediaType.APPLICATION_JSON).content(body))

    @Test
    fun `POST venta sells the flights and repeats the result for the same operacionId`() {
        val ida = vuelo(disponibilidad = 5)
        val vuelta = vuelo(disponibilidad = 5)
        val operacionId = UUID.randomUUID()
        val body = """{"operacionId":"$operacionId","vueloIds":[$ida,$vuelta]}"""

        vender(body)
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.operacionId").value(operacionId.toString()))
            .andExpect(jsonPath("$.vueloIds.length()").value(2))
            .andExpect(jsonPath("$.creadaEn").exists())

        vender(body)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.operacionId").value(operacionId.toString()))
    }

    @Test
    fun `POST venta returns 409 when a flight has no availability`() {
        val ida = vuelo(disponibilidad = 0)

        vender("""{"operacionId":"${UUID.randomUUID()}","vueloIds":[$ida]}""")
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST venta returns 404 when a flight does not exist`() {
        vender("""{"operacionId":"${UUID.randomUUID()}","vueloIds":[999999]}""")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST venta rejects an empty flight list or a missing operacionId`() {
        val ida = vuelo(disponibilidad = 5)

        vender("""{"operacionId":"${UUID.randomUUID()}","vueloIds":[]}""").andExpect(status().isBadRequest)
        vender("""{"vueloIds":[$ida]}""").andExpect(status().isBadRequest)
    }
}
