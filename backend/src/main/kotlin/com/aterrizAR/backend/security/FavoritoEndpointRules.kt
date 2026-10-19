package com.aterrizAR.backend.security

import com.aterrizAR.backend.model.Roles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

@Configuration(proxyBeanMethods = false)
class FavoritoEndpointRules {
    @Bean fun favoritoListRule() = EndpointRoleRule(HttpMethod.GET, "/api/favoritos", setOf(Roles.COMPRADOR))
    @Bean fun favoritoSaveRule() = EndpointRoleRule(HttpMethod.PUT, "/api/favoritos/{paqueteId}", setOf(Roles.COMPRADOR))
    @Bean fun favoritoDeleteRule() = EndpointRoleRule(HttpMethod.DELETE, "/api/favoritos/{paqueteId}", setOf(Roles.COMPRADOR))
}
