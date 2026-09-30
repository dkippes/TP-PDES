package com.aterrizAR.backend.security

import com.aterrizAR.backend.PingLogRepository
import com.aterrizAR.backend.auth.JwtService
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.model.Roles
import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.time.Instant
import javax.crypto.spec.SecretKeySpec

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "rbac-fixture")
@Import(RoleAuthorizationTest.Fixtures::class)
class RoleAuthorizationTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtEncoder: JwtEncoder
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var guardedService: GuardedService
    @Autowired lateinit var pingLogRepository: PingLogRepository
    @MockitoBean lateinit var restTemplate: RestTemplate

    @AfterEach
    fun cleanUp() {
        SecurityContextHolder.clearContext()
        pingLogRepository.deleteAll()
    }

    private fun token(
        role: Any? = Roles.COMPRADOR,
        expired: Boolean = false,
        encoder: JwtEncoder = jwtEncoder,
    ): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder().subject("1")
            .issuedAt(now.minusSeconds(300))
            .expiresAt(if (expired) now.minusSeconds(120) else now.plusSeconds(300))
        if (role != null) claims.claim("role", role)
        return encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims.build(),
        )).tokenValue
    }

    @Test
    fun `each signed role accesses only its explicitly allowed endpoint`() {
        val endpoints = mapOf(
            Roles.COMPRADOR to "/api/rbac-fixture/buyer",
            Roles.AGENTE to "/api/rbac-fixture/agent",
            Roles.ADMINISTRADOR to "/api/rbac-fixture/admin",
        )
        endpoints.forEach { (role, _) ->
            val jwt = token(role)
            endpoints.forEach { (allowedRole, path) ->
                mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer $jwt"))
                    .andExpect(if (role == allowedRole) status().isOk else status().isForbidden)
            }
        }
    }

    @Test
    fun `production JWT issuance uses the profile role accepted by HTTP authorization`() {
        val profiles = listOf(
            Comprador() to "/api/rbac-fixture/buyer",
            Agente(Agencia(nombre = "Agency", email = "agency@example.com")) to "/api/rbac-fixture/agent",
            Administrador() to "/api/rbac-fixture/admin",
        )
        profiles.forEach { (profile, path) ->
            val user = Usuario(id = 1L, nombre = "Ana", apellido = "Test", correo = "ana@example.com",
                direccion = "Street", passwordHash = "hash", perfil = profile)
            mockMvc.perform(get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtService.createToken(user)}"))
                .andExpect(status().isOk)
        }
    }

    @Test
    fun `shared endpoint allows only its listed roles without admin inheritance`() {
        Roles.supported.forEach { role ->
            mockMvc.perform(get("/api/rbac-fixture/shared")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${token(role)}"))
                .andExpect(if (role == Roles.ADMINISTRADOR) status().isForbidden else status().isOk)
        }
    }

    @Test
    fun `missing token returns the JSON unauthorized contract`() {
        mockMvc.perform(get("/api/rbac-fixture/buyer"))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Authentication is required"))
    }

    @Test
    fun `expired malformed and wrong-signature tokens return unauthorized`() {
        val otherEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(
            SecretKeySpec("another-test-only-secret-of-at-least-32-bytes".toByteArray(), "HmacSHA256"),
        ))
        listOf(token(expired = true), "not-a-jwt", token(encoder = otherEncoder)).forEach { jwt ->
            mockMvc.perform(get("/api/rbac-fixture/buyer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt"))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.status").value(401))
        }
    }

    @Test
    fun `missing unknown prefixed and non-string roles grant no permission`() {
        listOf(null, "OWNER", "ROLE_COMPRADOR", "comprador", listOf(Roles.COMPRADOR)).forEach { role ->
            mockMvc.perform(get("/api/rbac-fixture/buyer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${token(role)}"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.status").value(403))
        }
    }

    @Test
    fun `undeclared existing endpoint is denied even to an administrator`() {
        Roles.supported.forEach { role ->
            mockMvc.perform(get("/api/rbac-fixture/undeclared")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${token(role)}"))
                .andExpect(status().isForbidden)
        }
        mockMvc.perform(get("/api/rbac-fixture/undeclared")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `allowing a path does not allow another HTTP method`() {
        mockMvc.perform(post("/api/rbac-fixture/buyer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}"))
            .andExpect(status().isForbidden)
        mockMvc.perform(post("/api/health")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${token(Roles.ADMINISTRADOR)}"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `service annotation denies an HTTP-authorized caller with the wrong role`() {
        mockMvc.perform(get("/api/rbac-fixture/method-buyer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${token(Roles.ADMINISTRADOR)}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Access is denied"))
        mockMvc.perform(get("/api/rbac-fixture/method-buyer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}"))
            .andExpect(status().isOk)
    }

    @Test
    fun `service proxies enforce all composed annotations without a controller`() {
        val operations = mapOf<String, () -> String>(
            Roles.COMPRADOR to { guardedService.buyer() },
            Roles.AGENTE to { guardedService.agent() },
            Roles.ADMINISTRADOR to { guardedService.admin() },
        )
        Roles.supported.forEach { role ->
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                "test", null, listOf(SimpleGrantedAuthority(Roles.authority(role))),
            )
            operations.forEach { (allowedRole, operation) ->
                if (role == allowedRole) assertEquals("ok", operation())
                else assertThrows(AccessDeniedException::class.java) { operation() }
            }
        }
    }

    @Test
    fun `service annotations require authentication even without the HTTP filter`() {
        SecurityContextHolder.clearContext()
        assertThrows(AuthenticationCredentialsNotFoundException::class.java) { guardedService.buyer() }
        assertThrows(AuthenticationCredentialsNotFoundException::class.java) { guardedService.agent() }
        assertThrows(AuthenticationCredentialsNotFoundException::class.java) { guardedService.admin() }
    }

    @Test
    fun `health ping and documentation stay public`() {
        `when`(restTemplate.getForObject("http://flying-service:8081/ping", Map::class.java))
            .thenReturn(mapOf("status" to "ok"))
        mockMvc.perform(get("/api/health")).andExpect(status().isOk)
        mockMvc.perform(post("/api/ping")).andExpect(status().isOk)
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
            .andExpect(jsonPath("$.security").doesNotExist())
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk)
    }

    @Test
    fun `allowed-origin CORS preflight does not require a token`() {
        mockMvc.perform(options("/api/rbac-fixture/buyer")
            .header(HttpHeaders.ORIGIN, "http://localhost:5173")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
    }

    @Test
    fun `invalid endpoint policies fail fast`() {
        assertThrows(IllegalArgumentException::class.java) {
            EndpointRoleRule(HttpMethod.GET, "/api/test", emptySet())
        }
        assertThrows(IllegalArgumentException::class.java) {
            EndpointRoleRule(HttpMethod.GET, "/api/test", setOf("ADMIN"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            EndpointRoleRule(HttpMethod.GET, "/internal", setOf(Roles.COMPRADOR))
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class Fixtures {
        @Bean fun buyerRule() = EndpointRoleRule(HttpMethod.GET, "/api/rbac-fixture/buyer", setOf(Roles.COMPRADOR))
        @Bean fun agentRule() = EndpointRoleRule(HttpMethod.GET, "/api/rbac-fixture/agent", setOf(Roles.AGENTE))
        @Bean fun adminRule() = EndpointRoleRule(HttpMethod.GET, "/api/rbac-fixture/admin", setOf(Roles.ADMINISTRADOR))
        @Bean fun sharedRule() = EndpointRoleRule(HttpMethod.GET, "/api/rbac-fixture/shared", setOf(Roles.COMPRADOR, Roles.AGENTE))
        @Bean fun methodBuyerRule() = EndpointRoleRule(HttpMethod.GET, "/api/rbac-fixture/method-buyer", Roles.supported)
        @Bean fun guardedService() = GuardedService()
        @Bean fun fixtureController(service: GuardedService) = FixtureController(service)
    }

    // Nested test-only types are never included in the production artifact.
    open class GuardedService {
        @SoloComprador open fun buyer() = "ok"
        @SoloAgente open fun agent() = "ok"
        @SoloAdministrador open fun admin() = "ok"
    }

    @RestController
    @Profile("rbac-fixture")
    class FixtureController(private val service: GuardedService) {
        @GetMapping("/api/rbac-fixture/buyer") fun buyer() = service.buyer()
        @GetMapping("/api/rbac-fixture/agent") fun agent() = service.agent()
        @GetMapping("/api/rbac-fixture/admin") fun admin() = service.admin()
        @GetMapping("/api/rbac-fixture/shared") fun shared() = "ok"
        @GetMapping("/api/rbac-fixture/method-buyer") fun methodBuyer() = service.buyer()
        @GetMapping("/api/rbac-fixture/undeclared") fun undeclared() = "must not execute"
    }
}
