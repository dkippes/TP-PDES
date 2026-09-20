package com.aterrizAR.backend

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun aterrizarOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("AterrizAR API")
                .description("API para la gestión de paquetes turísticos y compras.")
                .version("v1"),
        )
}
