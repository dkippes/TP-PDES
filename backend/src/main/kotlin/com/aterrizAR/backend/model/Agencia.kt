package com.aterrizAR.backend.model

import com.fasterxml.jackson.annotation.JsonIgnore
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
    // @JsonIgnore: son colecciones de solo-JPA (cascade), exponerlas serializa Paquete/Compra
    // que a su vez referencian de vuelta a esta agencia y produce una recursión infinita.
    @JsonIgnore
    @OneToMany(mappedBy = "agencia", cascade = [CascadeType.ALL])
    val paquetes: MutableList<Paquete> = mutableListOf(),
    @JsonIgnore
    @OneToMany(mappedBy = "agencia")
    val compras: MutableList<Compra> = mutableListOf(),
)
