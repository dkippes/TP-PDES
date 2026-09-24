package com.aterrizAR.backend.auth.config

import com.aterrizAR.backend.auth.repository.UsuarioRepository
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@Configuration
class DefaultUsersConfiguration {
    private val passwordEncoder = BCryptPasswordEncoder()

    @Bean
    fun defaultUsersSeeder(usuarioRepository: UsuarioRepository) = ApplicationRunner {
        ensureUser(
            usuarioRepository = usuarioRepository,
            correo = "admin@admin.com",
            nombre = "Admin",
            apellido = "Admin",
            perfil = Administrador(),
        )
        ensureUser(
            usuarioRepository = usuarioRepository,
            correo = "user@user.com",
            nombre = "User",
            apellido = "User",
            perfil = Perfil(),
        )
    }

    private fun ensureUser(
        usuarioRepository: UsuarioRepository,
        correo: String,
        nombre: String,
        apellido: String,
        perfil: Perfil,
    ) {
        val existingUser = usuarioRepository.findByCorreo(correo)
        if (existingUser == null) {
            usuarioRepository.save(
                Usuario(
                    nombre = nombre,
                    apellido = apellido,
                    correo = correo,
                    direccion = "Sin especificar",
                    passwordHash = passwordEncoder.encode(DEFAULT_PASSWORD),
                    perfil = perfil,
                )
            )
            return
        }

        if (!passwordEncoder.matches(DEFAULT_PASSWORD, existingUser.passwordHash)) {
            usuarioRepository.updatePasswordHash(correo, passwordEncoder.encode(DEFAULT_PASSWORD))
        }
    }

    private companion object {
        const val DEFAULT_PASSWORD = "1234"
    }
}
