package com.aterrizAR.backend.auth

import com.aterrizAR.backend.model.Usuario
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class JwtService(
    private val jwtEncoder: JwtEncoder,
    @Value("\${app.jwt.expiration-ms}") expirationMs: Long,
) {
    private val expiration = Duration.ofMillis(expirationMs)
    val expiresInSeconds: Long = expiration.seconds

    init {
        require(!expiration.isNegative && !expiration.isZero) { "JWT expiration must be positive" }
    }

    fun createToken(usuario: Usuario, role: String): String {
        val issuedAt = Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject(requireNotNull(usuario.id).toString())
            .claim("correo", usuario.correo)
            .claim("role", role)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(expiration))
            .build()
        val headers = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue
    }
}
