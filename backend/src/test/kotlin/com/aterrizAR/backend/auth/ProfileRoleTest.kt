package com.aterrizAR.backend.auth

import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProfileRoleTest {
    @Test
    fun `profile roles map to unchanged DTO values`() {
        val profiles: List<Pair<Perfil, String>> = listOf(
            Comprador() to "COMPRADOR",
            Agente(Agencia(nombre = "Agency", email = "agency@example.com")) to "AGENTE",
            Administrador() to "ADMINISTRADOR",
        )

        profiles.forEach { (profile, expected) ->
            assertEquals(expected, profile.roleName)
            val user = Usuario(
                id = 1L,
                nombre = "Ana",
                apellido = "Test",
                correo = "ana@example.com",
                direccion = "Street",
                passwordHash = "hash",
                perfil = profile,
            )
            assertEquals(expected, user.toDTO().role)
        }
    }
}
