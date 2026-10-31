package com.aterrizAR.backend.security

import com.aterrizAR.backend.model.Roles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

@Configuration(proxyBeanMethods = false)
class ValoracionEndpointRules {
    @Bean fun valoracionListRule() = EndpointRoleRule(HttpMethod.GET, "/api/valoraciones/mias", setOf(Roles.COMPRADOR))
    @Bean fun valoracionSaveRule() = EndpointRoleRule(HttpMethod.PUT, "/api/compras/{compraId}/valoracion", setOf(Roles.COMPRADOR))
    @Bean fun valoracionDeleteRule() = EndpointRoleRule(HttpMethod.DELETE, "/api/compras/{compraId}/valoracion", setOf(Roles.COMPRADOR))
}
