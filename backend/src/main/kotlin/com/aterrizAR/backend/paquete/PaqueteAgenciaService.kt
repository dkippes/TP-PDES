package com.aterrizAR.backend.paquete

import com.aterrizAR.backend.model.Roles
import com.aterrizAR.backend.security.UsuarioAutenticado
import com.aterrizAR.backend.usuario.UsuarioRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/**
 * Escritura de paquetes con la regla de pertenencia: un AGENTE solo gestiona los paquetes de su propia agencia,
 * mientras que ADMINISTRADOR gestiona los de cualquiera. Las validaciones del paquete en sí viven en [PaqueteService].
 */
@Service
class PaqueteAgenciaService(
    private val paqueteService: PaqueteService,
    private val usuarioRepository: UsuarioRepository,
) {
    fun create(request: PaqueteRequest, autenticado: UsuarioAutenticado): PaqueteDTO {
        validarAgencia(request.agenciaId, autenticado)
        return paqueteService.create(request)
    }

    fun update(id: Long, request: PaqueteRequest, autenticado: UsuarioAutenticado): PaqueteDTO {
        validarPaquete(id, autenticado)
        validarAgencia(request.agenciaId, autenticado)
        return paqueteService.update(id, request)
    }

    fun delete(id: Long, autenticado: UsuarioAutenticado) {
        validarPaquete(id, autenticado)
        paqueteService.delete(id)
    }

    private fun validarPaquete(id: Long, autenticado: UsuarioAutenticado) {
        val paquete = paqueteService.findById(id)
        validarAgencia(requireNotNull(paquete.agencia.id), autenticado)
    }

    private fun validarAgencia(agenciaId: Long, autenticado: UsuarioAutenticado) {
        if (autenticado.rol == Roles.ADMINISTRADOR) return
        val gestiona = usuarioRepository.findByIdOrNull(autenticado.id)?.perfil?.gestionaAgencia(agenciaId) == true
        if (!gestiona) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puede gestionar paquetes de su propia agencia")
        }
    }
}
