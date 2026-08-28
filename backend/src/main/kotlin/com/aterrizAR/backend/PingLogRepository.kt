package com.aterrizAR.backend

import org.springframework.data.jpa.repository.JpaRepository

interface PingLogRepository : JpaRepository<PingLog, Long>
