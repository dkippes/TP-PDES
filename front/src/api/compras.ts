import type { JsonRequest } from './http'
import type { CompraRequestDTO, CompraResponseDTO } from '../types/compra'

/** One purchase attempt: every confirmation, including retries, sends the same operation id. */
export interface CompraIntento {
  readonly paqueteId: number
  readonly operacionId: string
  confirmar(): Promise<CompraResponseDTO>
}

export interface ComprasApi {
  iniciar(paqueteId: number): CompraIntento
}

export function createComprasApi(request: JsonRequest, generarId: () => string = () => crypto.randomUUID()): ComprasApi {
  return {
    iniciar: (paqueteId) => {
      const body: CompraRequestDTO = { paqueteId, operacionId: generarId() }
      return { ...body, confirmar: () => request<CompraResponseDTO>('POST', 'compras', body) }
    },
  }
}
