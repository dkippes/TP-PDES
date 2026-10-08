package com.aterrizAR.backend.agencia

import com.aterrizAR.backend.model.Agencia
import org.springframework.data.jpa.repository.JpaRepository

interface AgenciaRepository : JpaRepository<Agencia, Long> {
    fun existsByEmailAndIdNot(email: String, id: Long): Boolean
}
