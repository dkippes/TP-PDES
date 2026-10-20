package com.aterrizAR.backend.security

import com.aterrizAR.backend.model.Roles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

@Configuration(proxyBeanMethods = false)
class CompraEndpointRules {
    @Bean fun compraCreateRule() = EndpointRoleRule(HttpMethod.POST, "/api/compras", setOf(Roles.COMPRADOR))
}
