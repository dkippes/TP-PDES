package com.aterrizAR.backend

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "ping_log")
data class PingLog(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val timestamp: Instant = Instant.now(),

    val backendStatus: String = "ok",

    val flyingStatus: String = "unknown",

    val flyingResponse: String? = null
)
