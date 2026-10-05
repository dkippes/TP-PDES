package com.aterrizAR.backend.security

import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.usuario.UsuarioRepository
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ErrorStatusPropagationTest {

    @Value("\${local.server.port}")
    var port: Int = 0

    @Autowired
    lateinit var jwtService: JwtService

    @Autowired
    lateinit var usuarioRepository: UsuarioRepository

    private val client = HttpClient.newHttpClient()

    @AfterEach
    fun cleanUp() {
        usuarioRepository.deleteAll()
    }

    private fun adminToken(): String {
        val admin = usuarioRepository.save(Usuario(
            nombre = "Admin", apellido = "Test", correo = "admin-errors@example.com",
            direccion = "Quilmes", passwordHash = "unused", perfil = Administrador(),
        ))
        return jwtService.createToken(admin)
    }

    private fun send(method: String, path: String, token: String, body: String? = null): HttpResponse<String> {
        val publisher = body?.let { HttpRequest.BodyPublishers.ofString(it) } ?: HttpRequest.BodyPublishers.noBody()
        val request = HttpRequest.newBuilder(URI.create("http://localhost:$port$path"))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .method(method, publisher)
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `service not-found errors keep their 404 status and message`() {
        val token = adminToken()

        val usuario = send("GET", "/api/usuarios/999999", token)
        val agencia = send("GET", "/api/agencias/999999", token)

        assertEquals(404, usuario.statusCode())
        assertTrue(usuario.body().contains("Usuario 999999 no encontrado"))
        assertEquals(404, agencia.statusCode())
    }

    @Test
    fun `duplicate correo keeps its 409 status and message`() {
        val token = adminToken()
        usuarioRepository.save(Usuario(
            nombre = "Ana", apellido = "Perez", correo = "duplicado@example.com",
            direccion = "Quilmes", passwordHash = "unused", perfil = Comprador(),
        ))

        val response = send(
            "POST", "/api/usuarios", token,
            """{"nombre":"Ana","apellido":"Perez","correo":"DUPLICADO@example.com","direccion":"Quilmes","password":"12345678","rol":"COMPRADOR"}""",
        )

        assertEquals(409, response.statusCode())
        assertTrue(response.body().contains("Ya existe un usuario con ese correo"))
    }

    @Test
    fun `invalid query parameters and bodies keep their 400 status`() {
        val token = adminToken()

        assertEquals(400, send("GET", "/api/usuarios?rol=SUPERUSUARIO", token).statusCode())
        assertEquals(400, send("POST", "/api/usuarios", token, """{"nombre":"Ana"}""").statusCode())
    }

    @Test
    fun `unknown endpoints are still denied`() {
        assertEquals(403, send("GET", "/api/no-existe", adminToken()).statusCode())
    }
}
