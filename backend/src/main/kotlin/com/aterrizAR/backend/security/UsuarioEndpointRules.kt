package com.aterrizAR.backend.security

import com.aterrizAR.backend.model.Roles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

@Configuration(proxyBeanMethods = false)
class UsuarioEndpointRules {
    @Bean fun usuarioMeRule() = EndpointRoleRule(HttpMethod.GET, "/api/usuarios/me", Roles.supported)
    @Bean fun usuarioMeUpdateRule() = EndpointRoleRule(HttpMethod.PUT, "/api/usuarios/me", Roles.supported)
    @Bean fun usuarioMePasswordRule() = EndpointRoleRule(HttpMethod.PUT, "/api/usuarios/me/password", Roles.supported)

    @Bean fun usuarioListRule() = EndpointRoleRule(HttpMethod.GET, "/api/usuarios", setOf(Roles.ADMINISTRADOR))
    @Bean fun usuarioDetailRule() = EndpointRoleRule(HttpMethod.GET, "/api/usuarios/{id:[0-9]+}", setOf(Roles.ADMINISTRADOR))
    @Bean fun usuarioCreateRule() = EndpointRoleRule(HttpMethod.POST, "/api/usuarios", setOf(Roles.ADMINISTRADOR))
    @Bean fun usuarioUpdateRule() = EndpointRoleRule(HttpMethod.PUT, "/api/usuarios/{id:[0-9]+}", setOf(Roles.ADMINISTRADOR))
    @Bean fun usuarioDeleteRule() = EndpointRoleRule(HttpMethod.DELETE, "/api/usuarios/{id:[0-9]+}", setOf(Roles.ADMINISTRADOR))
}
