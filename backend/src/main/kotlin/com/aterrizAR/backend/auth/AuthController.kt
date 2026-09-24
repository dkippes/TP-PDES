package com.aterrizAR.backend.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration and authentication")
class AuthController(
    private val authService: AuthService,
    private val refreshTokenCookieFactory: RefreshTokenCookieFactory,
    @org.springframework.beans.factory.annotation.Value("\${app.auth.refresh-cookie.name}") private val refreshCookieName: String,
) {
    @PostMapping("/register")
    @Operation(summary = "Register a buyer account")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Buyer account created"),
            ApiResponse(responseCode = "400", description = "Invalid request"),
            ApiResponse(responseCode = "409", description = "Email already registered"),
        ],
    )
    fun register(@Valid @RequestBody request: RegisterRequestDTO): ResponseEntity<PublicUserResponseDTO> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Authentication successful"),
            ApiResponse(responseCode = "400", description = "Invalid request"),
            ApiResponse(responseCode = "401", description = "Invalid credentials"),
        ],
    )
    fun login(@Valid @RequestBody request: LoginRequestDTO): ResponseEntity<LoginResponseDTO> =
        withRefreshCookie(authService.login(request))

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token and issue a new access token")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Token refreshed"),
            ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
        ],
    )
    fun refresh(
        @CookieValue(name = "\${app.auth.refresh-cookie.name}", required = false) refreshToken: String?,
    ): ResponseEntity<LoginResponseDTO> = withRefreshCookie(authService.refresh(refreshToken ?: throw InvalidRefreshTokenException()))

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current refresh session")
    @ApiResponse(responseCode = "200", description = "Refresh session revoked or already absent")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        authService.logout(request.cookies?.firstOrNull { it.name == refreshCookieName }?.value)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString())
            .build()
    }

    private fun withRefreshCookie(session: AuthenticatedSession): ResponseEntity<LoginResponseDTO> = ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.create(session.rawRefreshToken).toString())
        .body(session.response)

}
