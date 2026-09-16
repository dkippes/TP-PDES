package com.backend.flying.service.vuelo

import org.springframework.data.jpa.repository.JpaRepository

interface VueloRepository : JpaRepository<Vuelo, Long>
