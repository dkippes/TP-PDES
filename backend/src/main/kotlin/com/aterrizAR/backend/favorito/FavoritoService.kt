package com.aterrizAR.backend.favorito

import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Favorito
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.paquete.PaqueteRepository
import com.aterrizAR.backend.security.SoloComprador
import com.aterrizAR.backend.security.UsuarioAutenticado
import com.aterrizAR.backend.usuario.UsuarioRepository
import com.aterrizAR.backend.usuario.comoComprador
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@SoloComprador
@Transactional(readOnly = true)
class FavoritoService(
    private val favoritoRepository: FavoritoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val paqueteRepository: PaqueteRepository,
) {
    fun listar(autenticado: UsuarioAutenticado): List<Favorito> {
        val comprador = compradorActual(usuarioRepository.findByIdOrNull(autenticado.id))
        return favoritoRepository.findAllDelComprador(requireNotNull(comprador.id))
    }

    @Transactional
    fun guardar(paqueteId: Long, autenticado: UsuarioAutenticado) {
        // Serializa cambios de favoritos de la misma cuenta, incluso desde pestañas distintas.
        val comprador = compradorActual(usuarioRepository.findByIdForUpdate(autenticado.id))
        val paquete = paqueteRepository.findByIdOrNull(paqueteId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Paquete $paqueteId no encontrado")
        if (!favoritoRepository.existsByComprador_IdAndPaquete_Id(requireNotNull(comprador.id), paqueteId)) {
            favoritoRepository.save(Favorito(comprador = comprador, paquete = paquete))
        }
    }

    @Transactional
    fun quitar(paqueteId: Long, autenticado: UsuarioAutenticado) {
        val comprador = compradorActual(usuarioRepository.findByIdForUpdate(autenticado.id))
        favoritoRepository.deleteByComprador_IdAndPaquete_Id(requireNotNull(comprador.id), paqueteId)
    }

    private fun compradorActual(usuario: Usuario?): Comprador = usuario.comoComprador("gestionar favoritos")
}
