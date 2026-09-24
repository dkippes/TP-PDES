package com.aterrizAR.backend.auth

import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Usuario
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
    private val refreshTokenService: RefreshTokenService,
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

    @Transactional
    fun login(request: LoginRequestDTO): AuthenticatedSession {
        val usuario = usuarioRepository.findByCorreo(request.correo.trim().lowercase(Locale.ROOT))
        if (usuario == null || !passwordEncoder.matches(request.password, usuario.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return authenticatedSession(usuario, refreshTokenService.issueFor(usuario))
    }

    @Transactional
    fun refresh(rawRefreshToken: String): AuthenticatedSession {
        val rotation = refreshTokenService.rotate(rawRefreshToken)
        return authenticatedSession(rotation.usuario, rotation.rawToken)
    }

    @Transactional
    fun logout(rawRefreshToken: String?) {
        refreshTokenService.revoke(rawRefreshToken)
    }

    private fun authenticatedSession(usuario: Usuario, rawRefreshToken: String): AuthenticatedSession {
        val publicUser = usuario.toDTO()
        return AuthenticatedSession(
            response = LoginResponseDTO(
                accessToken = jwtService.createToken(usuario, publicUser.role),
                expiresIn = jwtService.expiresInSeconds,
                user = publicUser,
            ),
            rawRefreshToken = rawRefreshToken,
        )
    }
}

data class AuthenticatedSession(
    val response: LoginResponseDTO,
    val rawRefreshToken: String,
)

class DuplicateEmailException : RuntimeException()
class InvalidCredentialsException : RuntimeException()
