package com.aterrizAR.backend.bootstrap

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Agente
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
class DevDataSeeder(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val agenciaRepository: AgenciaRepository,
) : CommandLineRunner {
    @Transactional
    override fun run(vararg args: String) {
        val sur = agenciaIfMissing("Viajes del Sur", "contacto@viajesdelsur.example")
        val andina = agenciaIfMissing("Rumbo Andino", "contacto@rumboandino.example")
        agenciaIfMissing("Horizonte Turismo", "contacto@horizonte.example")
        createIfMissing("admin@gmail.com", "Admin", Administrador())
        createIfMissing("usuario@gmail.com", "Usuario", Comprador())
        createIfMissing("agente@gmail.com", "Agente Sur", Agente(sur))
        createIfMissing("agente.andino@gmail.com", "Agente Andino", Agente(andina))
    }

    private fun agenciaIfMissing(nombre: String, email: String): Agencia =
        agenciaRepository.findByEmail(email) ?: agenciaRepository.save(Agencia(nombre = nombre, email = email))

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
