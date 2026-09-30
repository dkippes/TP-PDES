package com.aterrizAR.backend.model

object Roles {
    const val COMPRADOR = "COMPRADOR"
    const val AGENTE = "AGENTE"
    const val ADMINISTRADOR = "ADMINISTRADOR"

    val supported: Set<String> = setOf(COMPRADOR, AGENTE, ADMINISTRADOR)

    fun authority(role: String): String {
        require(role in supported) { "Unsupported role: $role" }
        return "ROLE_$role"
    }
}
