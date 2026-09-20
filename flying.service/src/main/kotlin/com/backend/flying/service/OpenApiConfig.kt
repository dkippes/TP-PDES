package com.backend.flying.service

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun flyingServiceOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Flying Service API")
                .description("API para la gestión de vuelos.")
                .version("v1"),
        )
}
