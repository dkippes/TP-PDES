package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.hotel.HotelRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Hotel
import com.aterrizAR.backend.model.Paquete
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
class PaqueteBusquedaServiceTest {

    @Autowired
    lateinit var paqueteService: PaqueteService

    @Autowired
    lateinit var paqueteRepository: PaqueteRepository

    @Autowired
    lateinit var hotelRepository: HotelRepository

    @Autowired
    lateinit var agenciaRepository: AgenciaRepository

    private lateinit var agenciaSur: Agencia
    private lateinit var agenciaNorte: Agencia
    private lateinit var barilocheSur: Paquete
    private lateinit var madridNorte: Paquete
    private lateinit var cordobaNorte: Paquete
    private lateinit var barilocheMendoza: Paquete

    @BeforeEach
    fun setUp() {
        val sheraton = hotelRepository.save(Hotel(nombre = "Sheraton", pais = "Argentina", foto = "sheraton.jpg"))
        val riu = hotelRepository.save(Hotel(nombre = "Riu Plaza", pais = "España", foto = "riu.jpg"))
        agenciaSur = agenciaRepository.save(Agencia(nombre = "Agencia Sur", email = "busqueda-sur@test.com"))
        agenciaNorte = agenciaRepository.save(Agencia(nombre = "Viajes Norte", email = "busqueda-norte@test.com"))

        barilocheSur = paquete(1000f, sheraton, agenciaSur, "Bariloche", "Buenos Aires", "2026-12-01", "2026-12-10")
        madridNorte = paquete(3000f, riu, agenciaNorte, "Madrid", "Buenos Aires", "2027-01-10", "2027-01-20")
        cordobaNorte = paquete(500f, sheraton, agenciaNorte, "Cordoba", "Mendoza", "2026-11-05", "2026-11-15")
        barilocheMendoza = paquete(2000f, riu, agenciaSur, "Bariloche", "Mendoza", "2027-02-01", "2027-02-10")
    }

    @AfterEach
    fun cleanUp() {
        paqueteRepository.deleteAll()
        hotelRepository.deleteAll()
        agenciaRepository.deleteAll()
    }

    private var nextVuelo = 1L

    private fun paquete(
        precio: Float,
        hotel: Hotel,
        agencia: Agencia,
        destino: String,
        origen: String,
        desde: String,
        hasta: String,
    ) = paqueteRepository.save(
        Paquete(
            precio = precio,
            vueloIdaId = nextVuelo++,
            vueloVueltaId = nextVuelo++,
            hotel = hotel,
            agencia = agencia,
            destino = destino,
            origen = origen,
            fechaInicial = LocalDate.parse(desde),
            fechaFinal = LocalDate.parse(hasta),
        ),
    )

    private fun ids(filtros: PaqueteBusquedaDTO) = paqueteService.buscar(filtros).content.map { it.id }

    @Test
    fun `without filters returns every paquete`() {
        val page = paqueteService.buscar(PaqueteBusquedaDTO())

        assertEquals(4, page.totalElements)
        assertEquals(4, page.content.size)
    }

    @Test
    fun `destino matches ignoring case`() {
        assertEquals(
            listOf(barilocheSur.id, barilocheMendoza.id),
            ids(PaqueteBusquedaDTO(destino = "bARILOCHE")),
        )
    }

    @Test
    fun `destino requires an exact match`() {
        assertEquals(emptyList<Long?>(), ids(PaqueteBusquedaDTO(destino = "Barilo")))
    }

    @Test
    fun `origen matches ignoring case`() {
        assertEquals(
            listOf(barilocheSur.id, madridNorte.id),
            ids(PaqueteBusquedaDTO(origen = "BUENOS AIRES")),
        )
    }

    @Test
    fun `agenciaId filters by agencia`() {
        assertEquals(
            listOf(madridNorte.id, cordobaNorte.id).toSet(),
            ids(PaqueteBusquedaDTO(agenciaId = agenciaNorte.id)).toSet(),
        )
        assertEquals(emptyList<Long?>(), ids(PaqueteBusquedaDTO(agenciaId = 999_999L)))
    }

    @Test
    fun `precio range is inclusive on both bounds`() {
        assertEquals(
            listOf(barilocheSur.id, barilocheMendoza.id),
            ids(PaqueteBusquedaDTO(precioMin = 1000f, precioMax = 2000f, orden = OrdenPaquete.PRECIO_ASC)),
        )
    }

    @Test
    fun `precioMin alone and precioMax alone filter one side`() {
        assertEquals(
            listOf(barilocheMendoza.id, madridNorte.id),
            ids(PaqueteBusquedaDTO(precioMin = 2000f, orden = OrdenPaquete.PRECIO_ASC)),
        )
        assertEquals(
            listOf(cordobaNorte.id),
            ids(PaqueteBusquedaDTO(precioMax = 999f)),
        )
    }

    @Test
    fun `fechaDesde filters on fechaInicial and fechaHasta on fechaFinal inclusively`() {
        assertEquals(
            listOf(barilocheSur.id, madridNorte.id, barilocheMendoza.id),
            ids(PaqueteBusquedaDTO(fechaDesde = LocalDate.of(2026, 12, 1))),
        )
        assertEquals(
            listOf(cordobaNorte.id, barilocheSur.id, madridNorte.id),
            ids(PaqueteBusquedaDTO(fechaHasta = LocalDate.of(2027, 1, 20))),
        )
    }

    @Test
    fun `q matches hotel nombre case insensitively`() {
        assertEquals(setOf(barilocheSur.id, cordobaNorte.id), ids(PaqueteBusquedaDTO(q = "SHERATON")).toSet())
    }

    @Test
    fun `q matches hotel pais`() {
        assertEquals(setOf(barilocheSur.id, cordobaNorte.id), ids(PaqueteBusquedaDTO(q = "argentina")).toSet())
        assertEquals(setOf(madridNorte.id, barilocheMendoza.id), ids(PaqueteBusquedaDTO(q = "España")).toSet())
    }

    @Test
    fun `q matches agencia nombre`() {
        assertEquals(setOf(madridNorte.id, cordobaNorte.id), ids(PaqueteBusquedaDTO(q = "viajes norte")).toSet())
    }

    @Test
    fun `q matches destino and origen by contains`() {
        assertEquals(listOf(madridNorte.id), ids(PaqueteBusquedaDTO(q = "MADR")))
        assertEquals(setOf(cordobaNorte.id, barilocheMendoza.id), ids(PaqueteBusquedaDTO(q = "mendo")).toSet())
    }

    @Test
    fun `q treats LIKE wildcards literally`() {
        assertEquals(emptyList<Long?>(), ids(PaqueteBusquedaDTO(q = "%")))
        assertEquals(emptyList<Long?>(), ids(PaqueteBusquedaDTO(q = "_")))
    }

    @Test
    fun `q without matches returns an empty page`() {
        val page = paqueteService.buscar(PaqueteBusquedaDTO(q = "inexistente"))

        assertEquals(0, page.totalElements)
        assertEquals(emptyList<Paquete>(), page.content)
    }

    @Test
    fun `filters are combined with AND`() {
        assertEquals(
            listOf(barilocheSur.id),
            ids(PaqueteBusquedaDTO(destino = "bariloche", precioMax = 1500f, agenciaId = agenciaSur.id)),
        )
        assertEquals(
            emptyList<Long?>(),
            ids(PaqueteBusquedaDTO(q = "sheraton", destino = "Madrid")),
        )
    }

    @Test
    fun `blank texts are ignored`() {
        assertEquals(4, paqueteService.buscar(PaqueteBusquedaDTO(q = "   ", destino = "", origen = " ")).totalElements)
    }

    @Test
    fun `texts are trimmed before matching`() {
        assertEquals(listOf(madridNorte.id), ids(PaqueteBusquedaDTO(destino = "  madrid ")))
    }

    @Test
    fun `each orden sorts as documented`() {
        fun ordered(orden: OrdenPaquete) = ids(PaqueteBusquedaDTO(orden = orden))

        assertEquals(
            listOf(cordobaNorte.id, barilocheSur.id, madridNorte.id, barilocheMendoza.id),
            ordered(OrdenPaquete.FECHA_ASC),
        )
        assertEquals(
            listOf(barilocheMendoza.id, madridNorte.id, barilocheSur.id, cordobaNorte.id),
            ordered(OrdenPaquete.FECHA_DESC),
        )
        assertEquals(
            listOf(cordobaNorte.id, barilocheSur.id, barilocheMendoza.id, madridNorte.id),
            ordered(OrdenPaquete.PRECIO_ASC),
        )
        assertEquals(
            listOf(madridNorte.id, barilocheMendoza.id, barilocheSur.id, cordobaNorte.id),
            ordered(OrdenPaquete.PRECIO_DESC),
        )
        // Bariloche empata: el id desempata de forma estable.
        assertEquals(
            listOf(barilocheSur.id, barilocheMendoza.id, cordobaNorte.id, madridNorte.id),
            ordered(OrdenPaquete.DESTINO_ASC),
        )
    }

    @Test
    fun `default orden is FECHA_ASC`() {
        assertEquals(OrdenPaquete.FECHA_ASC, PaqueteBusquedaDTO().orden)
        assertEquals(0, PaqueteBusquedaDTO().pagina)
        assertEquals(12, PaqueteBusquedaDTO().tamanio)
    }

    @Test
    fun `paginates with totals and a partial last page`() {
        val first = paqueteService.buscar(PaqueteBusquedaDTO(tamanio = 3, pagina = 0))
        val last = paqueteService.buscar(PaqueteBusquedaDTO(tamanio = 3, pagina = 1))

        assertEquals(3, first.content.size)
        assertEquals(4, first.totalElements)
        assertEquals(2, first.totalPages)
        assertEquals(listOf(barilocheMendoza.id), last.content.map { it.id })
        assertEquals(1, last.number)
    }

    @Test
    fun `page beyond the last one is empty but keeps totals`() {
        val page = paqueteService.buscar(PaqueteBusquedaDTO(tamanio = 3, pagina = 5))

        assertEquals(emptyList<Paquete>(), page.content)
        assertEquals(4, page.totalElements)
        assertEquals(2, page.totalPages)
    }

    @Test
    fun `inverted precio range is rejected with 400`() {
        val exception = assertThrows(ResponseStatusException::class.java) {
            paqueteService.buscar(PaqueteBusquedaDTO(precioMin = 3000f, precioMax = 100f))
        }
        assertEquals(400, exception.statusCode.value())
    }

    @Test
    fun `inverted fecha range is rejected with 400`() {
        val exception = assertThrows(ResponseStatusException::class.java) {
            paqueteService.buscar(
                PaqueteBusquedaDTO(fechaDesde = LocalDate.of(2027, 1, 1), fechaHasta = LocalDate.of(2026, 1, 1)),
            )
        }
        assertEquals(400, exception.statusCode.value())
    }
}
