package com.aterrizAR.backend.auth.service

import com.aterrizAR.backend.auth.dto.LoginRequest
import com.aterrizAR.backend.auth.dto.RegistroRequest
import com.aterrizAR.backend.auth.dto.UsuarioAutenticadoResponse
import com.aterrizAR.backend.auth.repository.UsuarioRepository
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(private val usuarioRepository: UsuarioRepository) {
    private val passwordEncoder = BCryptPasswordEncoder()

    @Transactional
    fun registrar(request: RegistroRequest): UsuarioAutenticadoResponse {
        val correo = normalizarCorreo(request.correo)
        if (usuarioRepository.existsByCorreo(correo)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo")
        }

        val usuario = usuarioRepository.save(
            Usuario(
                nombre = request.nombre.trim(),
                apellido = request.apellido.trim(),
                correo = correo,
                direccion = request.direccion.trim(),
                passwordHash = passwordEncoder.encode(request.password),
                perfil = Perfil(),
            )
        )
        return usuario.toResponse()
    }

    @Transactional(readOnly = true)
    fun iniciarSesion(request: LoginRequest): UsuarioAutenticadoResponse {
        val usuario = usuarioRepository.findByCorreo(normalizarCorreo(request.correo))
            ?: throw credencialesInvalidas()
        if (!passwordEncoder.matches(request.password, usuario.passwordHash)) {
            throw credencialesInvalidas()
        }
        return usuario.toResponse()
    }

    private fun normalizarCorreo(correo: String) = correo.trim().lowercase()

    private fun credencialesInvalidas() =
        ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos")

    private fun Usuario.toResponse() = UsuarioAutenticadoResponse(
        id = requireNotNull(id),
        nombre = nombre,
        correo = correo,
    )
}
