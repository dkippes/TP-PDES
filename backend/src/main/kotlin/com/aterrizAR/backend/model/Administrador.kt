package com.aterrizAR.backend.model

import jakarta.persistence.Entity

@Entity
class Administrador : Perfil() {
    override val roleName: String
        get() = "ADMINISTRADOR"
}
