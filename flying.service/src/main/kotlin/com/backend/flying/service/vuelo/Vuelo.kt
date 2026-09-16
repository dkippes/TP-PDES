package com.backend.flying.service.vuelo

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "vuelo")
class Vuelo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 120)
    val aerolinea: String,

    @Column(nullable = false)
    val fecha: LocalDate,

    @Column(nullable = false)
    val hora: LocalTime,

    @Column(nullable = false, length = 100)
    val origen: String,

    @Column(nullable = false, length = 100)
    val destino: String,

    @Column(nullable = false)
    val capacidad: Int,

    @Column(nullable = false)
    val disponibilidad: Int,
)
