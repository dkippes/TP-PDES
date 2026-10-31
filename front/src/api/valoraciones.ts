import type { JsonRequest } from './http'
import type { ValoracionPropiaDTO, ValoracionRequestDTO, ValoracionesPaqueteDTO } from '../types/valoracion'

export interface ValoracionesPublicasApi {
  listar(paqueteId: number): Promise<ValoracionesPaqueteDTO>
}

export interface ValoracionesApi {
  listarPropias(): Promise<ValoracionPropiaDTO[]>
  guardar(compraId: number, datos: ValoracionRequestDTO): Promise<ValoracionPropiaDTO>
  eliminar(compraId: number): Promise<void>
}

export function createValoracionesPublicasApi(request: JsonRequest): ValoracionesPublicasApi {
  return { listar: (paqueteId) => request('GET', `paquetes/${paqueteId}/valoraciones`) }
}

export function createValoracionesApi(request: JsonRequest): ValoracionesApi {
  return {
    listarPropias: () => request('GET', 'valoraciones/mias'),
    guardar: (compraId, datos) => request('PUT', `compras/${compraId}/valoracion`, datos),
    eliminar: (compraId) => request('DELETE', `compras/${compraId}/valoracion`),
  }
}
