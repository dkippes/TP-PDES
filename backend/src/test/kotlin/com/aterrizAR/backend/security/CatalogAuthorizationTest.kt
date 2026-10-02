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

    private val profiles = listOf(
        Comprador(), Agente(Agencia(nombre = "Test", email = "test@example.com")), Administrador(),
    )

    @Test
    fun `catalog endpoints require authentication`() {
        for (catalog in listOf("hoteles", "paquetes")) {
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
            for (catalog in listOf("hoteles", "paquetes")) {
                mockMvc.perform(get("/api/$catalog").authenticatedAs(jwtService, perfil))
                    .andExpect(status().isOk)
                mockMvc.perform(get("/api/$catalog/999").authenticatedAs(jwtService, perfil))
                    .andExpect(status().isNotFound)
            }
        }
    }

    @Test
    fun `buyers cannot write either catalog and agents cannot write hotels`() {
        for ((catalog, forbiddenProfiles) in mapOf(
            "hoteles" to profiles.take(2),
            "paquetes" to profiles.take(1),
        )) {
            for (perfil in forbiddenProfiles) {
                for ((method, path) in listOf(
                    HttpMethod.POST to "/api/$catalog",
                    HttpMethod.PUT to "/api/$catalog/999",
                    HttpMethod.DELETE to "/api/$catalog/999",
                )) {
                    mockMvc.perform(request(method, path).authenticatedAs(jwtService, perfil))
                        .andExpect(status().isForbidden)
                }
            }
        }
    }

    @Test
    fun `agents may write packages and administrators may write both catalogs`() {
        for ((catalog, allowedProfiles) in mapOf(
            "hoteles" to profiles.takeLast(1),
            "paquetes" to profiles.takeLast(2),
        )) {
            for (perfil in allowedProfiles) {
                // Missing bodies reach MVC validation (400); missing entities reach the service (404).
                mockMvc.perform(request(HttpMethod.POST, "/api/$catalog").authenticatedAs(jwtService, perfil))
                    .andExpect(status().isBadRequest)
                mockMvc.perform(request(HttpMethod.PUT, "/api/$catalog/999").authenticatedAs(jwtService, perfil))
                    .andExpect(status().isBadRequest)
                mockMvc.perform(request(HttpMethod.DELETE, "/api/$catalog/999").authenticatedAs(jwtService, perfil))
                    .andExpect(status().isNotFound)
            }
        }
    }

    @Test
    fun `unlisted methods and nested paths remain denied for administrators`() {
        for (catalog in listOf("hoteles", "paquetes")) {
            mockMvc.perform(request(HttpMethod.PATCH, "/api/$catalog/999").authenticatedAs(jwtService))
                .andExpect(status().isForbidden)
            mockMvc.perform(get("/api/$catalog/999/private").authenticatedAs(jwtService))
                .andExpect(status().isForbidden)
        }
    }

    @Test
    fun `OpenAPI documents catalog authentication and access errors`() {
        for (catalog in listOf("hoteles", "paquetes")) {
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
