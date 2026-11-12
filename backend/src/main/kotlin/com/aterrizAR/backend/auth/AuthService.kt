package com.aterrizAR.backend.auth

import com.aterrizAR.backend.agencia.AgenciaRepository
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.usuario.UsuarioRepository
import com.aterrizAR.backend.usuario.normalizarCorreo
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val usuarioRepository: UsuarioRepository,
    private val agenciaRepository: AgenciaRepository,
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

    /** Crea la agencia y a su primer agente en una sola transacción: si falla uno, no se guarda ninguno. */
    @Transactional
    fun registerAgencia(request: RegisterAgenciaRequestDTO): PublicUserResponseDTO {
        val agenciaToSave = request.agencia.toModel()
        if (usuarioRepository.existsByCorreo(normalizarCorreo(request.usuario.correo))) {
            throw DuplicateEmailException()
        }
        if (agenciaRepository.existsByEmail(agenciaToSave.email)) {
            throw agenciaEmailDuplicado()
        }

        val agencia = try {
            agenciaRepository.saveAndFlush(agenciaToSave)
        } catch (exception: DataIntegrityViolationException) {
            throw agenciaEmailDuplicado(exception)
        }
        val usuarioToSave = request.usuario.toModel(
            passwordHash = requireNotNull(passwordEncoder.encode(request.usuario.password)),
            perfil = Agente(agencia),
        )
        val usuario = try {
            usuarioRepository.saveAndFlush(usuarioToSave)
        } catch (_: DataIntegrityViolationException) {
            throw DuplicateEmailException()
        }
        return usuario.toDTO()
    }

    private fun agenciaEmailDuplicado(cause: Throwable? = null) =
        ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una agencia con ese email", cause)

    @Transactional
    fun login(request: LoginRequestDTO): AuthenticatedSession {
        val usuario = usuarioRepository.findByCorreo(normalizarCorreo(request.correo))
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
                accessToken = jwtService.createToken(usuario),
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
