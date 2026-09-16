package com.aterrizAR.backend.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne

@Entity
class Comprador(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @OneToOne(cascade = [CascadeType.ALL], optional = false)
    @JoinColumn(name = "perfil_id", nullable = false, unique = true)
    val perfil: Perfil,
    @OneToMany(mappedBy = "comprador")
    val compras: MutableList<Compra> = mutableListOf(),
    @OneToMany(mappedBy = "comprador", cascade = [CascadeType.ALL])
    val valoraciones: MutableList<Valoracion> = mutableListOf(),
)
