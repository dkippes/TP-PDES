package com.aterrizAR.backend.auth

import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.observability.NegocioMetrics
import com.aterrizAR.backend.observability.ResultadoLogin
import com.aterrizAR.backend.observability.ResultadoRefresh
import com.aterrizAR.backend.observability.ResultadoRegistro
import com.aterrizAR.backend.usuario.UsuarioRepository
import com.aterrizAR.backend.usuario.normalizarCorreo
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val metrics: NegocioMetrics,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun register(request: RegisterRequestDTO): PublicUserResponseDTO {
        val usuarioToSave = request.toModel(
            passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
            perfil = Comprador(),
        )
        if (usuarioRepository.existsByCorreo(usuarioToSave.correo)) {
            registroDuplicado()
        }

        val usuario = try {
            usuarioRepository.saveAndFlush(usuarioToSave)
        } catch (_: DataIntegrityViolationException) {
            registroDuplicado()
        }
        metrics.registro(ResultadoRegistro.EXITOSO)
        log.info("Usuario registrado: usuarioId={} rol={}", usuario.id, usuario.toDTO().role)
        return usuario.toDTO()
    }

    @Transactional
    fun login(request: LoginRequestDTO): AuthenticatedSession {
        val usuario = usuarioRepository.findByCorreo(normalizarCorreo(request.correo))
        if (usuario == null || !passwordEncoder.matches(request.password, usuario.passwordHash)) {
            // Neither the email nor the reason (unknown account vs wrong password) is logged: no enumeration, no PII.
            metrics.login(ResultadoLogin.FALLIDO)
            log.warn("Inicio de sesión fallido: credenciales inválidas")
            throw InvalidCredentialsException()
        }
        metrics.login(ResultadoLogin.EXITOSO)
        log.info("Inicio de sesión exitoso: usuarioId={} rol={}", usuario.id, usuario.toDTO().role)
        return authenticatedSession(usuario, refreshTokenService.issueFor(usuario))
    }

    @Transactional
    fun refresh(rawRefreshToken: String): AuthenticatedSession {
        val rotation = try {
            refreshTokenService.rotate(rawRefreshToken)
        } catch (exception: InvalidRefreshTokenException) {
            metrics.refresh(ResultadoRefresh.INVALIDO)
            throw exception
        }
        metrics.refresh(ResultadoRefresh.EXITOSO)
        return authenticatedSession(rotation.usuario, rotation.rawToken)
    }

    @Transactional
    fun logout(rawRefreshToken: String?) {
        refreshTokenService.revoke(rawRefreshToken)
    }

    private fun registroDuplicado(): Nothing {
        metrics.registro(ResultadoRegistro.DUPLICADO)
        log.warn("Registro rechazado: correo ya registrado")
        throw DuplicateEmailException()
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
