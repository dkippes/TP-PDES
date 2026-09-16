package com.aterrizAR.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class Paquete(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val precio: Float,
    @Column(name = "vuelo_ida_id", nullable = false)
    val vueloIdaId: Long,
    @Column(name = "vuelo_vuelta_id", nullable = false)
    val vueloVueltaId: Long,
    @ManyToOne(optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    val hotel: Hotel,
    @ManyToOne(optional = false)
    @JoinColumn(name = "agencia_id", nullable = false)
    val agencia: Agencia,
    @Column(nullable = false)
    val destino: String,
)
