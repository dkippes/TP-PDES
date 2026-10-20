import type { JsonRequest } from './http'
import type { AgenciaPublicaDTO, PaqueteBusquedaDTO, PaqueteResponseDTO, PaquetesPaginaDTO } from '../types/paquete'

export interface PaquetesApi {
  listar(): Promise<PaqueteResponseDTO[]>
  buscar(filtros: PaqueteBusquedaDTO): Promise<PaquetesPaginaDTO>
  listarAgencias(): Promise<AgenciaPublicaDTO[]>
}

export function createPaquetesApi(request: JsonRequest): PaquetesApi {
  return {
    listar: () => request('GET', 'paquetes'),
    buscar: (filtros) => {
      const params = new URLSearchParams()
      for (const [clave, valor] of Object.entries(filtros)) {
        const texto = typeof valor === 'string' ? valor.trim() : valor
        if (texto !== undefined && texto !== null && texto !== '') params.set(clave, String(texto))
      }
      const query = params.toString()
      return request('GET', query ? `paquetes/buscar?${query}` : 'paquetes/buscar')
    },
    listarAgencias: () => request('GET', 'agencias'),
  }
}
