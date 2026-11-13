package com.aterrizAR.backend.reporte

import com.aterrizAR.backend.compra.CompraRepository
import com.aterrizAR.backend.security.SoloAdministrador
import com.aterrizAR.backend.valoracion.ValoracionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
@SoloAdministrador
class ReporteAdminService(
    private val compraRepository: CompraRepository,
    private val valoracionRepository: ValoracionRepository,
) {
    // Los cuatro rankings deben observar los mismos datos aunque se registren compras o valoraciones en paralelo.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun obtener(): ReporteAdminDTO {
        val topCinco = PageRequest.of(0, 5)
        return ReporteAdminDTO(
            usuariosConMasCompras = compraRepository.findUsuariosConMasCompras(topCinco),
            destinosMejorRankeados = valoracionRepository.findDestinosMejorRankeados(topCinco),
            agenciasConMasVentas = compraRepository.findAgenciasConMasVentas(topCinco),
            destinosMasVendidos = compraRepository.findDestinosMasVendidos(topCinco),
        )
    }
}
