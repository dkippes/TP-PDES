package com.aterrizAR.backend.agencia

import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.auth.UsuarioRepository
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Roles
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AgenciaService(
    private val agenciaRepository: AgenciaRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    fun findAll(): List<Agencia> = agenciaRepository.findAll()

    fun findById(id: Long): Agencia = agenciaRepository.findById(id)
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Agencia $id no encontrada") }

    fun create(request: AgenciaRequestDTO): Agencia = save(request.toModel())

    @Transactional
    fun update(id: Long, request: AgenciaRequestDTO): Agencia {
        validateUpdatePermission(id)
        val agencia = findById(id)
        agencia.nombre = request.nombre
        agencia.email = request.email
        return save(agencia)
    }

    private fun validateUpdatePermission(id: Long) {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere autenticación")
        val authorities = authentication.authorities.map { it.authority }
        if (Roles.authority(Roles.ADMINISTRADOR) in authorities) return

        if (Roles.authority(Roles.AGENTE) in authorities) {
            val usuarioId = authentication.name.toLongOrNull()
            val usuario = usuarioId?.let { usuarioRepository.findById(it).orElse(null) }
            val agente = usuario?.perfil as? Agente
            if (agente != null && agente.agencia.id == id) return
        }
        throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puede modificar su propia agencia")
    }

    @Transactional
    fun delete(id: Long) {
        val agencia = findById(id)
        try {
            agenciaRepository.delete(agencia)
            agenciaRepository.flush()
        } catch (exception: DataIntegrityViolationException) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT, "La agencia $id tiene referencias que impiden eliminarla", exception,
            )
        }
    }

    private fun save(agencia: Agencia): Agencia = try {
        agenciaRepository.saveAndFlush(agencia)
    } catch (exception: DataIntegrityViolationException) {
        throw ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una agencia con ese email", exception)
    }
}
