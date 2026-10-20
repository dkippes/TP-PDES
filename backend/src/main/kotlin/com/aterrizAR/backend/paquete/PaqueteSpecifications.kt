package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.model.Paquete
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate

// Un predicado por filtro; los que no vienen quedan como no-op y se combinan con AND.
object PaqueteSpecifications {
    fun from(filtros: PaqueteBusquedaDTO): Specification<Paquete> = Specification.allOf(
        listOf(
            palabraClave(filtros.q),
            destino(filtros.destino),
            origen(filtros.origen),
            agencia(filtros.agenciaId),
            precioDesde(filtros.precioMin),
            precioHasta(filtros.precioMax),
            fechaDesde(filtros.fechaDesde),
            fechaHasta(filtros.fechaHasta),
        ),
    )

    fun palabraClave(q: String?): Specification<Paquete> = optional(q) { texto ->
        Specification { root, _, cb ->
            // hotel y agencia son ManyToOne: el join no duplica filas ni altera el count.
            val hotel = root.join<Paquete, Any>("hotel")
            val agencia = root.join<Paquete, Any>("agencia")
            val patron = "%${escaparLike(texto.lowercase())}%"
            cb.or(
                cb.like(cb.lower(root.get("destino")), patron, ESCAPE),
                cb.like(cb.lower(root.get("origen")), patron, ESCAPE),
                cb.like(cb.lower(hotel.get("nombre")), patron, ESCAPE),
                cb.like(cb.lower(hotel.get("pais")), patron, ESCAPE),
                cb.like(cb.lower(agencia.get("nombre")), patron, ESCAPE),
            )
        }
    }

    // % y _ del usuario se buscan literalmente, no como comodines de LIKE.
    private fun escaparLike(texto: String) = texto
        .replace("$ESCAPE", "$ESCAPE$ESCAPE")
        .replace("%", "$ESCAPE%")
        .replace("_", "${ESCAPE}_")

    private const val ESCAPE = '\\'

    fun destino(destino: String?): Specification<Paquete> = optional(destino) { valor ->
        Specification { root, _, cb -> igualIgnorandoCase(cb, root.get("destino"), valor) }
    }

    fun origen(origen: String?): Specification<Paquete> = optional(origen) { valor ->
        Specification { root, _, cb -> igualIgnorandoCase(cb, root.get("origen"), valor) }
    }

    fun agencia(agenciaId: Long?): Specification<Paquete> = optional(agenciaId) { id ->
        Specification { root, _, cb -> cb.equal(root.get<Any>("agencia").get<Long>("id"), id) }
    }

    fun precioDesde(precioMin: Float?): Specification<Paquete> = optional(precioMin) { min ->
        Specification { root, _, cb -> cb.greaterThanOrEqualTo(root.get<Float>("precio"), min) }
    }

    fun precioHasta(precioMax: Float?): Specification<Paquete> = optional(precioMax) { max ->
        Specification { root, _, cb -> cb.lessThanOrEqualTo(root.get<Float>("precio"), max) }
    }

    fun fechaDesde(fecha: LocalDate?): Specification<Paquete> = optional(fecha) { desde ->
        Specification { root, _, cb -> cb.greaterThanOrEqualTo(root.get<LocalDate>("fechaInicial"), desde) }
    }

    fun fechaHasta(fecha: LocalDate?): Specification<Paquete> = optional(fecha) { hasta ->
        Specification { root, _, cb -> cb.lessThanOrEqualTo(root.get<LocalDate>("fechaFinal"), hasta) }
    }

    private fun igualIgnorandoCase(cb: CriteriaBuilder, campo: Expression<String>, valor: String) =
        cb.equal(cb.lower(campo), valor.lowercase())

    private fun <T : Any> optional(valor: T?, build: (T) -> Specification<Paquete>): Specification<Paquete> =
        valor?.let(build) ?: Specification.unrestricted()
}
