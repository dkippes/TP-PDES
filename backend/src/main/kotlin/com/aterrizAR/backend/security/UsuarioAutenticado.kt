package com.aterrizAR.backend.security

import org.springframework.security.oauth2.jwt.Jwt

// JwtService emite el id del usuario como subject y su rol en el claim "role".
data class UsuarioAutenticado(val id: Long, val rol: String) {
    companion object {
        fun from(jwt: Jwt) = UsuarioAutenticado(
            id = requireNotNull(jwt.subject).toLong(),
            rol = requireNotNull(jwt.getClaimAsString("role")),
        )
    }
}
