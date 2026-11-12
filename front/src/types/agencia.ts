import type { PaqueteResponseDTO } from './paquete'

export type HotelDTO = { id: number; nombre: string; pais: string }

/** A package as the agency manages it: the catalog shape plus the flight and hotel IDs the form edits. */
export type PaqueteGestionDTO = PaqueteResponseDTO & {
  vueloIdaId: number
  vueloVueltaId: number
  hotel: HotelDTO
}

export type VueloDTO = {
  id: number
  origen: string
  destino: string
  fecha: string
  hora: string | null
  aerolinea: string
  /** Seats left on the flight. */
  disponibilidad: number
}

export type PaqueteRequestDTO = {
  precio: number
  vueloIdaId: number
  vueloVueltaId: number
  hotelId: number
  agenciaId: number
  destino: string
  origen: string
  fechaInicial: string
  fechaFinal: string
}

export type ClienteDTO = { id: number; nombre: string; apellido: string; correo: string }

export type VentaDTO = {
  id: number
  operacionId: string
  /** Price paid, fixed at purchase time. */
  precio: number
  fecha: string
  paqueteId: number
  origen: string
  destino: string
  hotel: string
  cliente: ClienteDTO
}

export type ClienteResumen = { cliente: ClienteDTO; compras: number; total: number; ultimaCompra: string }
