package com.aterrizAR.backend.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany

@Entity
class Comprador(
    @OneToMany(mappedBy = "comprador")
    val compras: MutableList<Compra> = mutableListOf(),
    @OneToMany(mappedBy = "comprador", cascade = [CascadeType.ALL])
    val valoraciones: MutableList<Valoracion> = mutableListOf(),
) : Perfil() {
    override val roleName: String
        get() = "COMPRADOR"
}
