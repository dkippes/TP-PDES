package com.aterrizAR.backend.reporte

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.compra.CompraRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Compra
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.model.Valoracion
import com.aterrizAR.backend.paquete.PaqueteRepository
import com.aterrizAR.backend.paquete.VueloClient
import com.aterrizAR.backend.security.authenticatedAs
import com.aterrizAR.backend.usuario.UsuarioRepository
import com.aterrizAR.backend.valoracion.ValoracionRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(properties = ["spring.datasource.url=jdbc:h2:mem:reportes_admin_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReporteAdminControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var entityManager: EntityManager
    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var agenciaRepository: AgenciaRepository
    @Autowired lateinit var hotelRepository: HotelRepository
    @Autowired lateinit var paqueteRepository: PaqueteRepository
    @Autowired lateinit var compraRepository: CompraRepository
    @Autowired lateinit var valoracionRepository: ValoracionRepository
    @MockitoBean lateinit var vueloClient: VueloClient

    @Test
    fun `solo administradores pueden consultar los reportes`() {
        val agente = Agente(Agencia(nombre = "Agencia", email = "agencia@example.com"))

        mockMvc.perform(get("/api/admin/reportes")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/admin/reportes").authenticatedAs(jwtService, Comprador()))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/api/admin/reportes").authenticatedAs(jwtService, agente))
            .andExpect(status().isForbidden)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `los rankings estan vacios cuando no hay actividad`() {
        verificarReportes("""
            {
              "usuariosConMasCompras": [],
              "destinosMejorRankeados": [],
              "agenciasConMasVentas": [],
              "destinosMasVendidos": []
            }
        """)
    }

    @Test
    fun `las resenas historicas rankean el destino aunque no tenga ventas`() {
        val paquete = paquete("Salta", agencia(1), hotel())
        valorar(paquete, comprador(1), 0)

        verificarReportes("""
            {
              "usuariosConMasCompras": [],
              "destinosMejorRankeados": [{"destino": "Salta", "promedio": 0.0, "totalValoraciones": 1}],
              "agenciasConMasVentas": [],
              "destinosMasVendidos": []
            }
        """)
    }

    @Test
    fun `cada ranking limita a cinco ordena empates y agrega sus propios datos`() {
        // Evita que coincidan los IDs de usuario y de perfil al comprobar la agrupación de compradores.
        entityManager.persist(Comprador())
        val compradores = (1..7).map(::comprador)
        val agencias = (1..7).map(::agencia)
        val hotel = hotel()
        val destinos = listOf("Madrid", "  Bariloche  ", "Mendoza", "Córdoba", "Ushuaia", "Rosario", "Tandil")
        val cantidades = listOf(2, 4, 1, 2, 1, 1, 1)
        val puntajes = listOf(8, 0, 6, 8, 1, 3, 2)
        destinos.indices.forEach { i ->
            val paquete = paquete(destinos[i], agencias[i], hotel)
            val compras = List(cantidades[i]) { comprar(compradores[i], paquete) }
            valorar(paquete, compradores[i], puntajes[i], compras.first())
            if (i == 1) {
                valorar(paquete, compradores[i], 10, compras[1])
                valorar(paquete, compradores[i], 9, compras[2])
            }
        }

        val otroBariloche = paquete("bariloche", agencias[2], hotel)
        // La venta conserva la agencia de la compra aunque la agencia del paquete haya cambiado.
        comprar(compradores[1], otroBariloche, agencia = agencias[0])
        valorar(otroBariloche, compradores[0], 9)

        verificarReportes("""
            {
              "usuariosConMasCompras": [
                {"usuarioId": ${compradores[1].id}, "nombre": "Comprador 2", "apellido": "Pérez", "totalCompras": 5},
                {"usuarioId": ${compradores[0].id}, "nombre": "Comprador 1", "apellido": "Pérez", "totalCompras": 2},
                {"usuarioId": ${compradores[3].id}, "nombre": "Comprador 4", "apellido": "Pérez", "totalCompras": 2},
                {"usuarioId": ${compradores[2].id}, "nombre": "Comprador 3", "apellido": "Pérez", "totalCompras": 1},
                {"usuarioId": ${compradores[4].id}, "nombre": "Comprador 5", "apellido": "Pérez", "totalCompras": 1}
              ],
              "destinosMejorRankeados": [
                {"destino": "Córdoba", "promedio": 8.0, "totalValoraciones": 1},
                {"destino": "Madrid", "promedio": 8.0, "totalValoraciones": 1},
                {"destino": "Bariloche", "promedio": 7.0, "totalValoraciones": 4},
                {"destino": "Mendoza", "promedio": 6.0, "totalValoraciones": 1},
                {"destino": "Rosario", "promedio": 3.0, "totalValoraciones": 1}
              ],
              "agenciasConMasVentas": [
                {"agenciaId": ${agencias[1].id}, "nombre": "Agencia 2", "totalVentas": 4},
                {"agenciaId": ${agencias[0].id}, "nombre": "Agencia 1", "totalVentas": 3},
                {"agenciaId": ${agencias[3].id}, "nombre": "Agencia 4", "totalVentas": 2},
                {"agenciaId": ${agencias[2].id}, "nombre": "Agencia 3", "totalVentas": 1},
                {"agenciaId": ${agencias[4].id}, "nombre": "Agencia 5", "totalVentas": 1}
              ],
              "destinosMasVendidos": [
                {"destino": "Bariloche", "totalVentas": 5},
                {"destino": "Córdoba", "totalVentas": 2},
                {"destino": "Madrid", "totalVentas": 2},
                {"destino": "Mendoza", "totalVentas": 1},
                {"destino": "Rosario", "totalVentas": 1}
              ]
            }
        """)
        verifyNoInteractions(vueloClient)
    }

    private fun verificarReportes(esperado: String) {
        mockMvc.perform(get("/api/admin/reportes").authenticatedAs(jwtService))
            .andExpect(status().isOk)
            .andExpect(content().json(esperado, JsonCompareMode.STRICT))
    }

    private fun comprador(numero: Int) = usuarioRepository.save(Usuario(
        nombre = "Comprador $numero", apellido = "Pérez", correo = "comprador$numero@example.com",
        direccion = "Quilmes", passwordHash = "unused", perfil = Comprador(),
    ))

    private fun agencia(numero: Int) = agenciaRepository.save(Agencia(
        nombre = "Agencia $numero", email = "agencia$numero@example.com",
    ))

    private fun hotel() = hotelRepository.save(Hotel(nombre = "Hotel Sur", pais = "Argentina", foto = ""))

    private fun paquete(destino: String, agencia: Agencia, hotel: Hotel) = paqueteRepository.save(Paquete(
        precio = 1500f, vueloIdaId = 1, vueloVueltaId = 2, hotel = hotel, agencia = agencia,
        origen = "Buenos Aires", destino = destino,
        fechaInicial = LocalDate.of(2027, 6, 1), fechaFinal = LocalDate.of(2027, 6, 8),
    ))

    private fun comprar(usuario: Usuario, paquete: Paquete, agencia: Agencia = paquete.agencia) = compraRepository.save(Compra(
        operacionId = UUID.randomUUID(), precio = paquete.precio, fecha = LocalDate.of(2027, 5, 1),
        paquete = paquete, agencia = agencia, comprador = usuario.perfil as Comprador,
    ))

    private fun valorar(paquete: Paquete, usuario: Usuario, puntaje: Int, compra: Compra? = null) =
        valoracionRepository.save(Valoracion(
            puntaje = puntaje, comentario = null, paquete = paquete, comprador = usuario.perfil as Comprador, compra = compra,
        ))
}
