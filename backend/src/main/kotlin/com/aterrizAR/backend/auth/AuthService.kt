package com.aterrizAR.backend.auth

import com.aterrizAR.backend.model.Comprador
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Service
class AuthService(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {
    @Transactional
    fun register(request: RegisterRequestDTO): PublicUserResponseDTO {
        val usuarioToSave = request.toModel(
            passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
            perfil = Comprador(),
        )
        if (usuarioRepository.existsByCorreo(usuarioToSave.correo)) {
            throw DuplicateEmailException()
        }

        val usuario = try {
            usuarioRepository.saveAndFlush(usuarioToSave)
        } catch (_: DataIntegrityViolationException) {
            throw DuplicateEmailException()
        }
        return usuario.toDTO()
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequestDTO): LoginResponseDTO {
        val usuario = usuarioRepository.findByCorreo(request.correo.trim().lowercase(Locale.ROOT))
        if (usuario == null || !passwordEncoder.matches(request.password, usuario.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val publicUser = usuario.toDTO()
        return LoginResponseDTO(
            accessToken = jwtService.createToken(usuario, publicUser.role),
            expiresIn = jwtService.expiresInSeconds,
            user = publicUser,
        )
    }

}

class DuplicateEmailException : RuntimeException()
class InvalidCredentialsException : RuntimeException()
