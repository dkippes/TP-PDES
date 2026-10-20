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
