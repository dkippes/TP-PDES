package com.aterrizAR.backend.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration and authentication")
class AuthController(
    private val authService: AuthService,
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
    fun login(@Valid @RequestBody request: LoginRequestDTO): LoginResponseDTO = authService.login(request)
}
