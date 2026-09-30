package com.aterrizAR.backend.security

import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

// Use production JWT issuance and the real HTTP security chain in CRUD tests.
fun MockHttpServletRequestBuilder.authenticatedAs(
    jwtService: JwtService,
    perfil: Perfil = Administrador(),
): MockHttpServletRequestBuilder {
    val usuario = Usuario(
        id = 1L, nombre = "Test", apellido = "User", correo = "test@example.com",
        direccion = "Quilmes", passwordHash = "unused", perfil = perfil,
    )
    return header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtService.createToken(usuario)}")
}
