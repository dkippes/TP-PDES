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
  paquete: PaqueteResponseDTO
}

/** What package cards need to offer buying and showing past purchases. */
export type CompraControls = {
  /** Times the buyer already bought each package. */
  comprados: ReadonlyMap<number, number>
  comprar: (paquete: PaqueteResponseDTO) => void
}
