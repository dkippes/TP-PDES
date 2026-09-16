package com.aterrizAR.backend.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

@Entity
class Agencia(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val nombre: String,
    @Column(nullable = false, unique = true)
    val email: String,
    @OneToMany(mappedBy = "agencia", cascade = [CascadeType.ALL])
    val paquetes: MutableList<Paquete> = mutableListOf(),
    @OneToMany(mappedBy = "agencia")
    val compras: MutableList<Compra> = mutableListOf(),
)
