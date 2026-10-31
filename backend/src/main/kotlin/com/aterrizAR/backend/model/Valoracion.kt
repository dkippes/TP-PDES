package com.aterrizAR.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import org.hibernate.annotations.ColumnDefault
import java.time.Instant

@Entity
class Valoracion(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    var puntaje: Int,
    @Column
    var comentario: String?,
    @ManyToOne(optional = false)
    @JoinColumn(name = "paquete_id", nullable = false)
    val paquete: Paquete,
    @ManyToOne(optional = false)
    @JoinColumn(name = "comprador_id", nullable = false)
    val comprador: Comprador,
    // Nullable solo para conservar valoraciones previas a la funcionalidad por compra.
    @OneToOne
    @JoinColumn(name = "compra_id", unique = true)
    val compra: Compra? = null,
    @Column(nullable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    val fecha: Instant = Instant.now(),
)
