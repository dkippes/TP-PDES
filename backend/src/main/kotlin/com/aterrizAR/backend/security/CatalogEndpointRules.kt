package com.aterrizAR.backend.security

import com.aterrizAR.backend.model.Roles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

@Configuration(proxyBeanMethods = false)
class CatalogEndpointRules {
    @Bean fun agenciaListRule() = EndpointRoleRule(HttpMethod.GET, "/api/agencias", Roles.supported)
    @Bean fun agenciaDetailRule() = EndpointRoleRule(HttpMethod.GET, "/api/agencias/{id}", Roles.supported)
    @Bean fun agenciaCreateRule() = EndpointRoleRule(HttpMethod.POST, "/api/agencias", setOf(Roles.ADMINISTRADOR))
    @Bean fun agenciaUpdateRule() = EndpointRoleRule(HttpMethod.PUT, "/api/agencias/{id}", setOf(Roles.ADMINISTRADOR, Roles.AGENTE))
    @Bean fun agenciaDeleteRule() = EndpointRoleRule(HttpMethod.DELETE, "/api/agencias/{id}", setOf(Roles.ADMINISTRADOR))

    @Bean fun hotelListRule() = EndpointRoleRule(HttpMethod.GET, "/api/hoteles", Roles.supported)
    @Bean fun hotelDetailRule() = EndpointRoleRule(HttpMethod.GET, "/api/hoteles/{id}", Roles.supported)
    @Bean fun hotelCreateRule() = EndpointRoleRule(HttpMethod.POST, "/api/hoteles", setOf(Roles.ADMINISTRADOR))
    @Bean fun hotelUpdateRule() = EndpointRoleRule(HttpMethod.PUT, "/api/hoteles/{id}", setOf(Roles.ADMINISTRADOR))
    @Bean fun hotelDeleteRule() = EndpointRoleRule(HttpMethod.DELETE, "/api/hoteles/{id}", setOf(Roles.ADMINISTRADOR))

    @Bean fun paqueteListRule() = EndpointRoleRule(HttpMethod.GET, "/api/paquetes", Roles.supported)
    @Bean fun paqueteDetailRule() = EndpointRoleRule(HttpMethod.GET, "/api/paquetes/{id}", Roles.supported)
    @Bean fun paqueteCreateRule() = EndpointRoleRule(HttpMethod.POST, "/api/paquetes", setOf(Roles.ADMINISTRADOR, Roles.AGENTE))
    @Bean fun paqueteUpdateRule() = EndpointRoleRule(HttpMethod.PUT, "/api/paquetes/{id}", setOf(Roles.ADMINISTRADOR, Roles.AGENTE))
    @Bean fun paqueteDeleteRule() = EndpointRoleRule(HttpMethod.DELETE, "/api/paquetes/{id}", setOf(Roles.ADMINISTRADOR, Roles.AGENTE))
}
