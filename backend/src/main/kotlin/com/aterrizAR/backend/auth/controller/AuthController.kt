package com.aterrizAR.backend.auth.controller

import com.aterrizAR.backend.auth.dto.LoginRequest
import com.aterrizAR.backend.auth.dto.RegistroRequest
import com.aterrizAR.backend.auth.dto.UsuarioAutenticadoResponse
import com.aterrizAR.backend.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    fun registrar(@Valid @RequestBody request: RegistroRequest): UsuarioAutenticadoResponse =
        authService.registrar(request)

    @PostMapping("/login")
    fun iniciarSesion(@Valid @RequestBody request: LoginRequest): UsuarioAutenticadoResponse =
        authService.iniciarSesion(request)
}
