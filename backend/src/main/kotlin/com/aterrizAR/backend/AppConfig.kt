package com.aterrizAR.backend

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AppConfig(
    @Value("\${app.cors.allowed-origin}") private val allowedOrigin: String
) {

    /**
     * Shared client for flying-service. Timeouts surface as ResourceAccessException (a RestClientException), so an
     * unresponsive service reads as "unavailable" instead of hanging catalog reads and purchases.
     */
    @Bean
    fun restTemplate(
        @Value("\${flying.service.connect-timeout-ms:2000}") connectTimeoutMs: Int,
        @Value("\${flying.service.read-timeout-ms:5000}") readTimeoutMs: Int,
    ): RestTemplate = RestTemplate(
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(connectTimeoutMs)
            setReadTimeout(readTimeoutMs)
        },
    )

    @Bean
    fun corsConfigurer(): WebMvcConfigurer = object : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            registry.addMapping("/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
        }
    }
}
