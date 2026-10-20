package com.aterrizAR.backend.compra

import com.aterrizAR.backend.model.Compra
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.paquete.PaqueteRepository
import com.aterrizAR.backend.paquete.ResultadoVentaVuelos
import com.aterrizAR.backend.paquete.VueloClient
import com.aterrizAR.backend.security.SoloComprador
import com.aterrizAR.backend.security.UsuarioAutenticado
import com.aterrizAR.backend.usuario.UsuarioRepository
import com.aterrizAR.backend.usuario.comoComprador
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID

@Service
@SoloComprador
class CompraService(
    private val compraRepository: CompraRepository,
    private val usuarioRepository: UsuarioRepository,
    private val paqueteRepository: PaqueteRepository,
    private val vueloClient: VueloClient,
) {
    /**
     * Sells the package flights and stores the purchase at the current package price. The flights are sold first:
     * if storing fails, retrying with the same [CompraRequestDTO.operacionId] gets the original sale back from flying-service.
     */
    @Transactional
    fun comprar(request: CompraRequestDTO, autenticado: UsuarioAutenticado): CompraRegistrada {
        val paqueteId = requireNotNull(request.paqueteId)
        val operacionId = requireNotNull(request.operacionId)
        // Serializa las compras de la misma cuenta: un reintento concurrente encuentra la compra ya guardada.
        val comprador = usuarioRepository.findByIdForUpdate(autenticado.id).comoComprador("comprar paquetes")
        compraRepository.findByOperacionId(operacionId)?.let { return repetir(it, paqueteId, comprador) }

        val paquete = paqueteRepository.findByIdOrNull(paqueteId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Paquete $paqueteId no encontrado")
        when (vueloClient.vender(operacionId, listOf(paquete.vueloIdaId, paquete.vueloVueltaId))) {
            ResultadoVentaVuelos.VENDIDA -> Unit
            ResultadoVentaVuelos.RECHAZADA ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "Los vuelos del paquete $paqueteId no están disponibles")
            ResultadoVentaVuelos.SERVICIO_NO_DISPONIBLE ->
                throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de vuelos no está disponible")
        }

        val compra = Compra(
            operacionId = operacionId,
            precio = paquete.precio,
            fecha = LocalDate.now(),
            paquete = paquete,
            agencia = paquete.agencia,
            comprador = comprador,
        )
        return try {
            CompraRegistrada(compraRepository.saveAndFlush(compra), nueva = true)
        } catch (exception: DataIntegrityViolationException) {
            // Otro comprador registró la misma operación entre la búsqueda y el guardado.
            throw operacionEnUso(operacionId, exception)
        }
    }

    private fun repetir(compra: Compra, paqueteId: Long, comprador: Comprador): CompraRegistrada {
        if (compra.comprador.id != comprador.id || compra.paquete.id != paqueteId) throw operacionEnUso(compra.operacionId)
        return CompraRegistrada(compra, nueva = false)
    }

    private fun operacionEnUso(operacionId: UUID, cause: Throwable? = null) =
        ResponseStatusException(HttpStatus.CONFLICT, "La operación $operacionId pertenece a otra compra", cause)
}

data class CompraRegistrada(val compra: Compra, val nueva: Boolean)
