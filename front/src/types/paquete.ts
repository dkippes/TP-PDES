// Fields consumed from GET /api/paquetes; the API also returns flight IDs, hotel photo and agency email.
export type PaqueteResponseDTO = {
  id: number
  origen: string
  destino: string
  precio: number
  fechaInicial: string
  fechaFinal: string
  hotel: { nombre: string; pais: string }
  agencia: { id: number; nombre: string }
}

export type PaqueteDetalleDTO = PaqueteResponseDTO & {
  hotel: PaqueteResponseDTO['hotel'] & { foto: string }
}

export const ordenesPaquete = ['FECHA_ASC', 'FECHA_DESC', 'PRECIO_ASC', 'PRECIO_DESC', 'DESTINO_ASC'] as const
export type OrdenPaquete = (typeof ordenesPaquete)[number]

export type PaqueteBusquedaDTO = {
  q?: string
  destino?: string
  origen?: string
  agenciaId?: number
  precioMin?: number
  precioMax?: number
  fechaDesde?: string
  fechaHasta?: string
  orden?: OrdenPaquete
  pagina?: number
  tamanio?: number
}

export type PaquetesPaginaDTO = {
  contenido: PaqueteResponseDTO[]
  pagina: number
  tamanio: number
  totalElementos: number
  totalPaginas: number
}

export type AgenciaPublicaDTO = { id: number; nombre: string }
