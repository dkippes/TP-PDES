export type ValoracionRequestDTO = { puntaje: number; comentario?: string }

export type ValoracionPublicaDTO = {
  id: number
  puntaje: number
  comentario: string | null
  fecha: string
}

export type ValoracionesPaqueteDTO = {
  promedio: number | null
  total: number
  valoraciones: ValoracionPublicaDTO[]
}

export type ValoracionPropiaDTO = { compraId: number; valoracion: ValoracionPublicaDTO }
