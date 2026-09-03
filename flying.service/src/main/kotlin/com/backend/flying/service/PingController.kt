package com.backend.flying.service

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class PingController {

    @GetMapping("/ping")
    fun ping(): Map<String, Any> = mapOf(
        "service" to "flying-service",
        "status" to "ok",
        "timestamp" to Instant.now().toString()
    )
}
