import type { JsonRequest } from './http'
import type { UsuarioResponseDTO } from '../types/admin'
import type { HotelDTO, PaqueteGestionDTO, PaqueteRequestDTO, VentaDTO, VueloDTO } from '../types/agencia'
import type { PaquetesPaginaDTO } from '../types/paquete'

export const TAMANIO_PAGINA_PAQUETES = 12

export interface PaquetesAgenciaApi {
  listar(agenciaId: number, pagina: number): Promise<Omit<PaquetesPaginaDTO, 'contenido'> & { contenido: PaqueteGestionDTO[] }>
  crear(datos: PaqueteRequestDTO): Promise<PaqueteGestionDTO>
  editar(id: number, datos: PaqueteRequestDTO): Promise<PaqueteGestionDTO>
  eliminar(id: number): Promise<void>
}

export interface AgenciaApi {
  /** The signed-in account, whose agenciaId says which agency the agent manages. */
  perfil(): Promise<UsuarioResponseDTO>
  hoteles(): Promise<HotelDTO[]>
  vuelos(): Promise<VueloDTO[]>
  paquetes: PaquetesAgenciaApi
  ventas(): Promise<VentaDTO[]>
}

export function createAgenciaApi(request: JsonRequest): AgenciaApi {
  return {
    perfil: () => request('GET', 'usuarios/me'),
    hoteles: () => request('GET', 'hoteles'),
    vuelos: () => request('GET', 'vuelos'),
    ventas: () => request('GET', 'ventas'),
    paquetes: {
      listar: (agenciaId, pagina) => request('GET',
        `paquetes/buscar?agenciaId=${agenciaId}&orden=FECHA_DESC&pagina=${pagina}&tamanio=${TAMANIO_PAGINA_PAQUETES}`),
      crear: (datos) => request('POST', 'paquetes', datos),
      editar: (id, datos) => request('PUT', `paquetes/${id}`, datos),
      eliminar: (id) => request('DELETE', `paquetes/${id}`),
    },
  }
}
