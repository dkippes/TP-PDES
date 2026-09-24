package com.aterrizAR.backend.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity
class Usuario(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val nombre: String,
    @Column(nullable = false)
    val apellido: String,
    @Column(nullable = false, unique = true)
    val correo: String,
    @Column(nullable = false)
    val direccion: String,
    @Column(name = "password_hash", nullable = false, length = 60)
    val passwordHash: String,
    @OneToOne(cascade = [CascadeType.ALL], optional = false)
    @JoinColumn(name = "perfil_id", nullable = false, unique = true)
    val perfil: Perfil,
)
