package com.aterrizAR.backend.security

import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Comprador
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogAuthorizationTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService

    private val comprador = Comprador()
    private val agente = Agente(Agencia(nombre = "Test", email = "test@example.com"))
    private val administrador = Administrador()
    private val profiles = listOf(comprador, agente, administrador)

    // Mirrors CatalogEndpointRules. Agency ownership for AGENTE updates is checked in AgenciaAuthorizationTest.
    private val writePermissions = mapOf(
        "hoteles" to mapOf(
            HttpMethod.POST to setOf(administrador),
            HttpMethod.PUT to setOf(administrador),
            HttpMethod.DELETE to setOf(administrador),
        ),
        "paquetes" to mapOf(
            HttpMethod.POST to setOf(agente, administrador),
            HttpMethod.PUT to setOf(agente, administrador),
            HttpMethod.DELETE to setOf(agente, administrador),
        ),
        "agencias" to mapOf(
            HttpMethod.POST to setOf(administrador),
            HttpMethod.PUT to setOf(agente, administrador),
            HttpMethod.DELETE to setOf(administrador),
        ),
    )

    @Test
    fun `catalog endpoints require authentication`() {
        for (catalog in listOf("hoteles", "paquetes", "agencias")) {
            for ((method, path) in listOf(
                HttpMethod.GET to "/api/$catalog",
                HttpMethod.GET to "/api/$catalog/999",
                HttpMethod.POST to "/api/$catalog",
                HttpMethod.PUT to "/api/$catalog/999",
                HttpMethod.DELETE to "/api/$catalog/999",
            )) {
                mockMvc.perform(request(method, path)).andExpect(status().isUnauthorized)
            }
        }
    }

    @Test
    fun `all supported roles may read lists and details`() {
        for (perfil in profiles) {
            for (catalog in listOf("hoteles", "paquetes", "agencias")) {
                mockMvc.perform(get("/api/$catalog").authenticatedAs(jwtService, perfil))
                    .andExpect(status().isOk)
                mockMvc.perform(get("/api/$catalog/999").authenticatedAs(jwtService, perfil))
                    .andExpect(status().isNotFound)
            }
        }
    }

    @Test
    fun `write endpoints admit only the roles declared for each method`() {
        // Allowed requests pass the filter chain: missing bodies reach MVC validation (400)
        // and missing entities reach the service (404). Denied requests stop at the filter (403).
        val allowedStatus = mapOf(
            HttpMethod.POST to status().isBadRequest,
            HttpMethod.PUT to status().isBadRequest,
            HttpMethod.DELETE to status().isNotFound,
        )
        for ((catalog, permissions) in writePermissions) {
            for ((method, allowedProfiles) in permissions) {
                val path = if (method == HttpMethod.POST) "/api/$catalog" else "/api/$catalog/999"
                for (perfil in profiles) {
                    val expected = if (perfil in allowedProfiles) allowedStatus.getValue(method) else status().isForbidden
                    mockMvc.perform(request(method, path).authenticatedAs(jwtService, perfil))
                        .andExpect(expected)
                }
            }
        }
    }

    @Test
    fun `unlisted methods and nested paths remain denied for administrators`() {
        for (catalog in listOf("hoteles", "paquetes", "agencias")) {
            mockMvc.perform(request(HttpMethod.PATCH, "/api/$catalog/999").authenticatedAs(jwtService))
                .andExpect(status().isForbidden)
            mockMvc.perform(get("/api/$catalog/999/private").authenticatedAs(jwtService))
                .andExpect(status().isForbidden)
        }
    }

    @Test
    fun `OpenAPI documents catalog authentication and access errors`() {
        for (catalog in listOf("hoteles", "paquetes", "agencias")) {
            for ((path, methods) in mapOf(
                "/api/$catalog" to listOf("get", "post"),
                "/api/$catalog/{id}" to listOf("get", "put", "delete"),
            )) {
                for (method in methods) {
                    val operation = "$['paths']['$path']['$method']"
                    mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$operation['security'][0]['bearerAuth']").isArray)
                        .andExpect(jsonPath("$operation['responses']['401']").exists())
                        .andExpect(jsonPath("$operation['responses']['403']").exists())
                }
            }
        }
    }
}
