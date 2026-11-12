package com.aterrizAR.backend.security

import com.aterrizAR.backend.model.Roles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

@Configuration(proxyBeanMethods = false)
class ReporteEndpointRules {
    @Bean fun reporteAdminRule() = EndpointRoleRule(HttpMethod.GET, "/api/admin/reportes", setOf(Roles.ADMINISTRADOR))
}
