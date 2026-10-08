package com.aterrizAR.backend.usuario

import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByCorreo(correo: String): Usuario?
    fun existsByCorreo(correo: String): Boolean
    fun existsByCorreoAndIdNot(correo: String, id: Long): Boolean

    // JOIN FETCH: el perfil es EAGER y sin él se dispara un SELECT por usuario (N+1).
    @Query("SELECT u FROM Usuario u JOIN FETCH u.perfil ORDER BY u.id")
    fun findAllConPerfil(): List<Usuario>

    // El rol es el subtipo concreto de Perfil (herencia JOINED), así que se filtra por TYPE en la base.
    @Query("SELECT u FROM Usuario u JOIN FETCH u.perfil p WHERE TYPE(p) = :tipoPerfil ORDER BY u.id")
    fun findAllByTipoPerfil(@Param("tipoPerfil") tipoPerfil: Class<out Perfil>): List<Usuario>
}
