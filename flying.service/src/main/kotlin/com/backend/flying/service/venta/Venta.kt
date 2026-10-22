package com.backend.flying.service.venta

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "venta")
class Venta(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "operacion_id", nullable = false, unique = true)
    val operacionId: UUID,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "venta_vuelo", joinColumns = [JoinColumn(name = "venta_id")])
    @Column(name = "vuelo_id", nullable = false)
    val vueloIds: List<Long>,

    @Column(name = "creada_en", nullable = false)
    val creadaEn: Instant = Instant.now(),
) {
    fun toDTO() = VentaDTO(operacionId = operacionId, vueloIds = vueloIds.sorted(), creadaEn = creadaEn)
}
