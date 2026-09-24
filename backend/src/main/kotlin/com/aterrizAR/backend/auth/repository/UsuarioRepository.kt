package com.aterrizAR.backend.auth.repository

import com.aterrizAR.backend.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun existsByCorreo(correo: String): Boolean
    fun findByCorreo(correo: String): Usuario?

    @Modifying
    @Transactional
    @Query("update Usuario usuario set usuario.passwordHash = :passwordHash where usuario.correo = :correo")
    fun updatePasswordHash(
        @Param("correo") correo: String,
        @Param("passwordHash") passwordHash: String,
    ): Int
}
