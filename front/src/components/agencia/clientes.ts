import type { ClienteResumen, VentaDTO } from '../../types/agencia'

/** One row per client with how many packages they bought and what they paid, most recent buyers first. */
export function resumirClientes(ventas: readonly VentaDTO[]): ClienteResumen[] {
  const porCliente = new Map<number, ClienteResumen>()
  for (const venta of ventas) {
    const actual = porCliente.get(venta.cliente.id)
    porCliente.set(venta.cliente.id, {
      cliente: venta.cliente,
      compras: (actual?.compras ?? 0) + 1,
      total: (actual?.total ?? 0) + venta.precio,
      // ISO dates compare as text.
      ultimaCompra: actual && actual.ultimaCompra > venta.fecha ? actual.ultimaCompra : venta.fecha,
    })
  }
  return [...porCliente.values()].sort((a, b) => b.ultimaCompra.localeCompare(a.ultimaCompra) || a.cliente.id - b.cliente.id)
}
