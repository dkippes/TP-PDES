package com.aterrizAR.backend.auth

import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.usuario.UsuarioRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("dev")
class DevUsersSeeder(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandLineRunner {
    @Transactional
    override fun run(vararg args: String) {
        createIfMissing("admin@gmail.com", "Admin", Administrador())
        createIfMissing("usuario@gmail.com", "Usuario", Comprador())
    }

    private fun createIfMissing(correo: String, nombre: String, perfil: Perfil) {
        if (usuarioRepository.existsByCorreo(correo)) return

        usuarioRepository.save(
            Usuario(
                nombre = nombre,
                apellido = "Demo",
                correo = correo,
                direccion = "Quilmes",
                passwordHash = requireNotNull(passwordEncoder.encode("12345678")),
                perfil = perfil,
            ),
        )
    }
}
