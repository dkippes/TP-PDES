import type { CompraResponseDTO } from '../../types/compra'
import type { PaqueteResponseDTO } from '../../types/paquete'

export type CompraGrupo = {
  paquete: PaqueteResponseDTO
  /** Same order as the input: newest first. */
  compras: CompraResponseDTO[]
  total: number
}

/** Groups purchases by package; groups keep the order of their most recent purchase. */
export function agruparCompras(compras: CompraResponseDTO[]): CompraGrupo[] {
  const grupos = new Map<number, CompraGrupo>()
  for (const compra of compras) {
    const grupo = grupos.get(compra.paquete.id)
    if (grupo) {
      grupo.compras.push(compra)
      grupo.total += compra.precio
    } else {
      grupos.set(compra.paquete.id, { paquete: compra.paquete, compras: [compra], total: compra.precio })
    }
  }
  return [...grupos.values()]
}
