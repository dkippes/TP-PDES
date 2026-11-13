package com.aterrizAR.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.OneToMany

@Entity
class Comprador(
    @OneToMany(mappedBy = "comprador")
    val compras: MutableList<Compra> = mutableListOf(),
    // Sin cascada: las valoraciones son contenido público y no deben borrarse junto con el perfil.
    // Eliminar un comprador con valoraciones falla por FK (409).
    @OneToMany(mappedBy = "comprador")
    val valoraciones: MutableList<Valoracion> = mutableListOf(),
) : Perfil() {
    override val roleName: String
        get() = Roles.COMPRADOR
}
