package com.aterrizAR.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["comprador_id", "paquete_id"])])
class Favorito(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(optional = false)
    @JoinColumn(name = "comprador_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val comprador: Comprador,
    @ManyToOne(optional = false)
    @JoinColumn(name = "paquete_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val paquete: Paquete,
    @Column(name = "agregado_en", nullable = false)
    val agregadoEn: Instant = Instant.now(),
)
