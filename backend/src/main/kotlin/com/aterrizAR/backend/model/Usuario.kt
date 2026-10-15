package com.aterrizAR.backend.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "usuario")
class Usuario(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    var nombre: String,
    @Column(nullable = false)
    var apellido: String,
    @Column(nullable = false, unique = true)
    var correo: String,
    @Column(nullable = false)
    var direccion: String,
    @Column(nullable = false, name = "password_hash")
    var passwordHash: String,
    // El perfil pertenece al usuario: cambiar de rol reemplaza el perfil y el anterior se elimina.
    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false, unique = true)
    var perfil: Perfil,
)
