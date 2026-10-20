import type { JsonRequest } from './http'
import type { FavoritoResponseDTO } from '../types/favorito'
import type { PaqueteResponseDTO } from '../types/paquete'

export interface FavoritosApi {
  listar(): Promise<PaqueteResponseDTO[]>
  guardar(paqueteId: number): Promise<void>
  quitar(paqueteId: number): Promise<void>
}

export function createFavoritosApi(request: JsonRequest): FavoritosApi {
  return {
    listar: async () => (await request<FavoritoResponseDTO[]>('GET', 'favoritos')).map(({ paquete }) => paquete),
    guardar: (paqueteId) => request('PUT', `favoritos/${paqueteId}`),
    quitar: (paqueteId) => request('DELETE', `favoritos/${paqueteId}`),
  }
}
