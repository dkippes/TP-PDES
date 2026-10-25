package com.aterrizAR.backend.valoracion

import com.aterrizAR.backend.compra.CompraRepository
import com.aterrizAR.backend.model.Compra
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Valoracion
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
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class ValoracionService(
    private val valoracionRepository: ValoracionRepository,
    private val compraRepository: CompraRepository,
    private val usuarioRepository: UsuarioRepository,
    private val paqueteRepository: PaqueteRepository,
) {
    fun listarPublicas(paqueteId: Long): ValoracionesPaqueteDTO {
        if (!paqueteRepository.existsById(paqueteId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Paquete no encontrado")
        }
        val valoraciones = valoracionRepository.findAllByPaquete_IdOrderByFechaDescIdDesc(paqueteId).map { it.toDTO() }
        return ValoracionesPaqueteDTO(
            promedio = valoraciones.takeIf { it.isNotEmpty() }?.map { it.puntaje }?.average(),
            total = valoraciones.size,
            valoraciones = valoraciones,
        )
    }

    @SoloComprador
    fun listarPropias(autenticado: UsuarioAutenticado): List<ValoracionPropiaDTO> {
        val comprador = usuarioRepository.findByIdOrNull(autenticado.id).comoComprador("ver sus valoraciones")
        return valoracionRepository.findAllPropias(requireNotNull(comprador.id)).map { it.toPropiaDTO() }
    }

    @SoloComprador
    @Transactional
    fun guardar(compraId: Long, request: ValoracionRequestDTO, autenticado: UsuarioAutenticado): ValoracionPropiaDTO {
        val puntaje = request.puntaje
        if (puntaje == null || puntaje < BigDecimal.ZERO || puntaje > BigDecimal.TEN ||
            puntaje.stripTrailingZeros().scale() > 0 || (request.comentario?.length ?: 0) > 255) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El puntaje debe estar entre 0 y 10 y el comentario no puede superar 255 caracteres")
        }
        // Serializa altas, ediciones y eliminaciones del comprador, incluso desde distintas pestanas.
        val comprador = usuarioRepository.findByIdForUpdate(autenticado.id).comoComprador("valorar compras")
        val compra = compraPropia(compraId, comprador)
        val comentario = request.comentario?.trim()?.takeIf { it.isNotEmpty() }
        val valoracion = valoracionRepository.findByCompra_Id(compraId)?.apply {
            this.puntaje = puntaje.toInt()
            this.comentario = comentario
        } ?: Valoracion(puntaje = puntaje.toInt(), comentario = comentario, paquete = compra.paquete, comprador = comprador, compra = compra)
        return valoracionRepository.saveAndFlush(valoracion).toPropiaDTO()
    }

    @SoloComprador
    @Transactional
    fun eliminar(compraId: Long, autenticado: UsuarioAutenticado) {
        val comprador = usuarioRepository.findByIdForUpdate(autenticado.id).comoComprador("eliminar sus valoraciones")
        compraPropia(compraId, comprador)
        valoracionRepository.findByCompra_Id(compraId)?.let { valoracionRepository.delete(it) }
    }

    private fun compraPropia(compraId: Long, comprador: Comprador): Compra =
        compraRepository.findByIdOrNull(compraId)?.takeIf { it.comprador.id == comprador.id }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada")
}
