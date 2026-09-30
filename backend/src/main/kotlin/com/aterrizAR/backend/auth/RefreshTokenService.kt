package com.aterrizAR.backend.auth

import com.aterrizAR.backend.model.Usuario
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${app.auth.refresh-token.expiration-ms}") expirationMs: Long,
) {
    private val expiration = Duration.ofMillis(expirationMs)
    private val secureRandom = SecureRandom()

    init {
        require(!expiration.isNegative && !expiration.isZero) { "Refresh token expiration must be positive" }
    }

    @Transactional
    fun issueFor(usuario: Usuario): String = createSession(usuario, UUID.randomUUID())

    @Transactional
    fun rotate(rawToken: String): RefreshTokenRotation {
        val now = Instant.now()
        val current = refreshTokenRepository.findByTokenHash(hash(rawToken)) ?: throw InvalidRefreshTokenException()

        if (current.revokedAt != null) {
            refreshTokenRepository.revokeActiveByFamilyId(current.familyId, now)
            throw InvalidRefreshTokenException()
        }
        if (!current.expiresAt.isAfter(now)) {
            current.revoke(now)
            throw InvalidRefreshTokenException()
        }

        current.revoke(now)
        val replacement = createSession(current.usuario, current.familyId)
        return RefreshTokenRotation(current.usuario, replacement)
    }

    @Transactional
    fun revoke(rawToken: String?) {
        if (rawToken.isNullOrBlank()) {
            return
        }

        val token = refreshTokenRepository.findByTokenHash(hash(rawToken)) ?: return
        token.revoke(Instant.now())
    }

    private fun createSession(usuario: Usuario, familyId: UUID): String {
        val rawToken = newRawToken()
        refreshTokenRepository.save(
            RefreshToken(
                tokenHash = hash(rawToken),
                usuario = usuario,
                familyId = familyId,
                expiresAt = Instant.now().plus(expiration),
            ),
        )
        return rawToken
    }

    private fun newRawToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

data class RefreshTokenRotation(
    val usuario: Usuario,
    val rawToken: String,
)

class InvalidRefreshTokenException : RuntimeException()
