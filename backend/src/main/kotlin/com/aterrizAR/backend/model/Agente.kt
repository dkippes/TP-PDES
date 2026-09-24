package com.aterrizAR.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class Agente(
    @ManyToOne(optional = false)
    @JoinColumn(name = "agencia_id", nullable = false)
    val agencia: Agencia,
) : Perfil() {
    override val roleName: String
        get() = "AGENTE"
}
