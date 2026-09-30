package com.aterrizAR.backend.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RefreshTokenCookieFactory(
    @Value("\${app.auth.refresh-cookie.name}") private val name: String,
    @Value("\${app.auth.refresh-cookie.path}") private val path: String,
    @Value("\${app.auth.refresh-cookie.same-site}") private val sameSite: String,
    @Value("\${app.auth.refresh-cookie.secure}") private val secure: Boolean,
    @Value("\${app.auth.refresh-token.expiration-ms}") expirationMs: Long,
) {
    private val maxAge = Duration.ofMillis(expirationMs)

    fun create(rawToken: String): ResponseCookie = ResponseCookie.from(name, rawToken)
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite)
        .path(path)
        .maxAge(maxAge)
        .build()

    fun clear(): ResponseCookie = ResponseCookie.from(name, "")
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite)
        .path(path)
        .maxAge(Duration.ZERO)
        .build()
}
