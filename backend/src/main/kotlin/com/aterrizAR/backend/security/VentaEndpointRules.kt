package com.aterrizAR.backend.security

import com.aterrizAR.backend.model.Roles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

@Configuration(proxyBeanMethods = false)
class VentaEndpointRules {
    @Bean fun ventaListRule() = EndpointRoleRule(HttpMethod.GET, "/api/ventas", setOf(Roles.AGENTE))
}
