package com.aterrizAR.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class Valoracion(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val puntaje: Int,
    @Column(nullable = false)
    val comentario: String,
    @ManyToOne(optional = false)
    @JoinColumn(name = "paquete_id", nullable = false)
    val paquete: Paquete,
    @ManyToOne(optional = false)
    @JoinColumn(name = "comprador_id", nullable = false)
    val comprador: Comprador,
)
