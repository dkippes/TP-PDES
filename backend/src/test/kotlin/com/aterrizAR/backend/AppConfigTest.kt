package com.aterrizAR.backend

import com.aterrizAR.backend.paquete.ResultadoVentaVuelos
import com.aterrizAR.backend.paquete.VueloClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException
import java.net.ServerSocket
import java.util.UUID

class AppConfigTest {
    private val config = AppConfig(allowedOrigin = "http://localhost:5173")

    @Test
    fun `a flying-service that accepts the connection but never answers times out as an unavailable service`() {
        // The server socket completes the TCP handshake (backlog) and never writes a response.
        ServerSocket(0).use { silent ->
            val restTemplate = config.restTemplate(connectTimeoutMs = 500, readTimeoutMs = 300)

            val started = System.nanoTime()
            val failure = runCatching {
                restTemplate.getForObject("http://127.0.0.1:${silent.localPort}/vuelos", String::class.java)
            }.exceptionOrNull()

            assertTrue(failure is ResourceAccessException, "expected ResourceAccessException but was $failure")
            assertTrue((System.nanoTime() - started) / 1_000_000 < 5_000, "the read timeout was not applied")
        }
    }

    @Test
    fun `a timed out availability read is unknown for the client`() {
        ServerSocket(0).use { silent ->
            val client = VueloClient(
                config.restTemplate(connectTimeoutMs = 500, readTimeoutMs = 300),
                "http://127.0.0.1:${silent.localPort}",
            )

            assertNull(client.findDisponibilidades(listOf(1L)))
            assertEquals(ResultadoVentaVuelos.SERVICIO_NO_DISPONIBLE, client.vender(UUID.randomUUID(), listOf(1L)))
        }
    }
}
