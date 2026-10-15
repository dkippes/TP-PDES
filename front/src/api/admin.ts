import type { JsonRequest } from './http'
import type {
  AgenciaRequestDTO, AgenciaResponseDTO, UsuarioCreateRequestDTO, UsuarioResponseDTO, UsuarioUpdateRequestDTO,
} from '../types/admin'

export interface UsuariosApi {
  listar(): Promise<UsuarioResponseDTO[]>
  crear(datos: UsuarioCreateRequestDTO): Promise<UsuarioResponseDTO>
  editar(id: number, datos: UsuarioUpdateRequestDTO): Promise<UsuarioResponseDTO>
  eliminar(id: number): Promise<void>
}

export interface AgenciasApi {
  listar(): Promise<AgenciaResponseDTO[]>
  crear(datos: AgenciaRequestDTO): Promise<AgenciaResponseDTO>
  editar(id: number, datos: AgenciaRequestDTO): Promise<AgenciaResponseDTO>
  eliminar(id: number): Promise<void>
}

export type AdminApi = { usuarios: UsuariosApi; agencias: AgenciasApi }

// Resource operations depend on an authenticated request, not on fetch or React state.
export function createAdminApi(request: JsonRequest): AdminApi {
  return {
    usuarios: {
      listar: () => request('GET', 'usuarios'),
      crear: (datos) => request('POST', 'usuarios', datos),
      editar: (id, datos) => request('PUT', `usuarios/${id}`, datos),
      eliminar: (id) => request('DELETE', `usuarios/${id}`),
    },
    agencias: {
      listar: () => request('GET', 'agencias'),
      crear: (datos) => request('POST', 'agencias', datos),
      editar: (id, datos) => request('PUT', `agencias/${id}`, datos),
      eliminar: (id) => request('DELETE', `agencias/${id}`),
    },
  }
}
