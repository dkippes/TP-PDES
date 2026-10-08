package com.aterrizAR.backend.agencia

import com.aterrizAR.backend.auth.UsuarioRepository
import com.aterrizAR.backend.model.Agencia
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Roles
import com.aterrizAR.backend.security.UsuarioAutenticado
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
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

    fun create(request: AgenciaRequestDTO): Agencia {
        val agencia = request.toModel()
        validateEmailUnico(agencia.email)
        return save(agencia)
    }

    @Transactional
    fun update(id: Long, request: AgenciaRequestDTO, usuario: UsuarioAutenticado): Agencia {
        // Se valida antes de buscar la agencia para que un AGENTE no pueda sondear qué ids existen.
        validateUpdatePermission(id, usuario)
        val agencia = findById(id)
        val datos = request.toModel()
        // Antes de modificar la entidad: la consulta dispararía un flush de los cambios pendientes.
        validateEmailUnicoExcepto(datos.email, id)
        agencia.nombre = datos.nombre
        agencia.email = datos.email
        return save(agencia)
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

    private fun validateUpdatePermission(id: Long, usuario: UsuarioAutenticado) {
        if (usuario.rol == Roles.ADMINISTRADOR) return
        val agente = usuarioRepository.findByIdOrNull(usuario.id)?.perfil as? Agente
        if (agente?.agencia?.id != id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puede modificar su propia agencia")
        }
    }

    private fun validateEmailUnico(email: String) {
        if (agenciaRepository.existsByEmail(email)) {
            throw emailDuplicado()
        }
    }

    private fun validateEmailUnicoExcepto(email: String, id: Long) {
        if (agenciaRepository.existsByEmailAndIdNot(email, id)) {
            throw emailDuplicado()
        }
    }

    // El UNIQUE de la base cubre la carrera entre la validación previa y el guardado.
    private fun save(agencia: Agencia): Agencia = try {
        agenciaRepository.saveAndFlush(agencia)
    } catch (exception: DataIntegrityViolationException) {
        throw emailDuplicado(exception)
    }

    private fun emailDuplicado(cause: Throwable? = null) =
        ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una agencia con ese email", cause)
}
