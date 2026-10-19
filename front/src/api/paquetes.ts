import type { JsonRequest } from './http'
import type { PaqueteResponseDTO } from '../types/paquete'

export interface PaquetesApi {
  listar(): Promise<PaqueteResponseDTO[]>
}

export function createPaquetesApi(request: JsonRequest): PaquetesApi {
  return { listar: () => request('GET', 'paquetes') }
}
