package com.aterrizAR.backend

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.time.Instant

@RestController
@RequestMapping("/api")
class PingController(
    private val restTemplate: RestTemplate,
    private val pingLogRepository: PingLogRepository,
    @Value("\${flying.service.url:http://flying-service:8081}") private val flyingServiceUrl: String
) {
    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "up")

    @PostMapping("/ping")
    fun ping(): ResponseEntity<Map<String, Any>> {
        val flyingResponse = try {
            restTemplate.getForObject("$flyingServiceUrl/ping", Map::class.java)
        } catch (e: Exception) {
            mapOf(
                "service" to "flying-service",
                "status" to "error",
                "error" to (e.message ?: "unknown")
            )
        }
        val flyingStatus = (flyingResponse?.get("status") as? String) ?: "unknown"
        val saved = pingLogRepository.save(
            PingLog(
                timestamp = Instant.now(),
                backendStatus = "ok",
                flyingStatus = flyingStatus,
                flyingResponse = flyingResponse.toString().take(1000)
            )
        )
        val response = mapOf(
            "service" to "backend",
            "status" to if (flyingStatus == "ok") "ok" else "degraded",
            "timestamp" to Instant.now().toString(),
            "flyingService" to (flyingResponse ?: mapOf("status" to "no response")),
            "persisted" to mapOf("id" to saved.id, "totalPings" to pingLogRepository.count())
        )
        val httpStatus = if (flyingStatus == "ok") HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
        return ResponseEntity.status(httpStatus).body(response)
    }
}
