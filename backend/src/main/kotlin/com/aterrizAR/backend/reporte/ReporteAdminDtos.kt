package com.aterrizAR.backend.reporte

data class ReporteAdminDTO(
    val usuariosConMasCompras: List<UsuarioComprasDTO>,
    val destinosMejorRankeados: List<DestinoValoracionesDTO>,
    val agenciasConMasVentas: List<AgenciaVentasDTO>,
    val destinosMasVendidos: List<DestinoVentasDTO>,
)

data class UsuarioComprasDTO(
    val usuarioId: Long,
    val nombre: String,
    val apellido: String,
    val totalCompras: Long,
)

data class DestinoValoracionesDTO(
    val destino: String,
    val promedio: Double,
    val totalValoraciones: Long,
)

data class AgenciaVentasDTO(
    val agenciaId: Long,
    val nombre: String,
    val totalVentas: Long,
)

data class DestinoVentasDTO(
    val destino: String,
    val totalVentas: Long,
)
