package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.model.Paquete
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

// Cada orden conoce su Sort; el id siempre desempata para que la paginación sea estable.
enum class OrdenPaquete(val sort: Sort) {
    FECHA_ASC(Sort.by("fechaInicial").ascending().and(Sort.by("id").ascending())),
    FECHA_DESC(Sort.by("fechaInicial").descending().and(Sort.by("id").ascending())),
    PRECIO_ASC(Sort.by("precio").ascending().and(Sort.by("id").ascending())),
    PRECIO_DESC(Sort.by("precio").descending().and(Sort.by("id").ascending())),
    DESTINO_ASC(Sort.by("destino").ascending().and(Sort.by("id").ascending())),
}

data class PaqueteBusquedaDTO(
    @field:Schema(description = "Palabra clave (contains, sin distinguir mayúsculas) en destino, origen, nombre/país del hotel y nombre de la agencia")
    val q: String? = null,

    @field:Schema(description = "Destino exacto, sin distinguir mayúsculas")
    val destino: String? = null,

    @field:Schema(description = "Origen exacto, sin distinguir mayúsculas")
    val origen: String? = null,

    @field:Positive
    @field:Schema(description = "Id de la agencia")
    val agenciaId: Long? = null,

    @field:PositiveOrZero
    @field:Schema(description = "Precio mínimo, inclusivo")
    val precioMin: Float? = null,

    @field:PositiveOrZero
    @field:Schema(description = "Precio máximo, inclusivo")
    val precioMax: Float? = null,

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @field:Schema(description = "Fecha inicial mínima (ISO yyyy-MM-dd), inclusiva")
    val fechaDesde: LocalDate? = null,

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @field:Schema(description = "Fecha final máxima (ISO yyyy-MM-dd), inclusiva")
    val fechaHasta: LocalDate? = null,

    @field:Schema(description = "Criterio de ordenamiento", defaultValue = "FECHA_ASC")
    val orden: OrdenPaquete = OrdenPaquete.FECHA_ASC,

    @field:Min(0)
    @field:Schema(description = "Número de página, desde 0", defaultValue = "0")
    val pagina: Int = 0,

    @field:Min(1)
    @field:Max(50)
    @field:Schema(description = "Elementos por página (1 a 50)", defaultValue = "12")
    val tamanio: Int = 12,
)

data class PaquetesPaginaDTO(
    val contenido: List<Paquete>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
)

fun Page<Paquete>.toDTO() = PaquetesPaginaDTO(
    contenido = content,
    pagina = number,
    tamanio = size,
    totalElementos = totalElements,
    totalPaginas = totalPages,
)
