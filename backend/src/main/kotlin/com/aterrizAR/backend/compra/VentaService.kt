package com.aterrizAR.backend.compra

import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.security.SoloAgente
import com.aterrizAR.backend.security.UsuarioAutenticado
import com.aterrizAR.backend.usuario.UsuarioRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@SoloAgente
class VentaService(
    private val compraRepository: CompraRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    /** Ventas de la agencia del agente autenticado, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    fun listar(autenticado: UsuarioAutenticado): List<VentaResponseDTO> {
        val usuario = usuarioRepository.findByIdOrNull(autenticado.id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario ya no existe")
        val agente = usuario.perfil as? Agente
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los agentes pueden consultar ventas")
        return compraRepository.findVentasDeLaAgencia(requireNotNull(agente.agencia.id))
    }
}
