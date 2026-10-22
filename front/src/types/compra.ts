import type { PaqueteResponseDTO } from './paquete'

export type CompraRequestDTO = {
  paqueteId: number
  operacionId: string
}

export type CompraResponseDTO = {
  id: number
  operacionId: string
  /** Price paid, fixed at purchase time. */
  precio: number
  fecha: string
  /** Purchases carry no availability: it only matters while a package can still be bought. */
  paquete: Omit<PaqueteResponseDTO, 'disponibilidad'>
}

/** What package cards need to offer buying and showing past purchases. */
export type CompraControls = {
  /** Times the buyer already bought each package. */
  comprados: ReadonlyMap<number, number>
  /** Availability refreshed after a purchase; it wins over the (older) value a package came with. */
  disponibilidades: ReadonlyMap<number, number | null>
  comprar: (paquete: PaqueteResponseDTO) => void
}
