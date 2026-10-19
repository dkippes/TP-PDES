export const roles = ['ADMINISTRADOR', 'AGENTE', 'COMPRADOR'] as const
export type Rol = typeof roles[number]

export function esRol(value: string): value is Rol {
  return roles.some((rol) => rol === value)
}

export type AgenciaResponseDTO = { id: number; nombre: string; email: string }
export type AgenciaRequestDTO = Omit<AgenciaResponseDTO, 'id'>

export type UsuarioResponseDTO = {
  id: number
  nombre: string
  apellido: string
  correo: string
  direccion: string
  rol: Rol
  agenciaId: number | null
}

export type UsuarioCreateRequestDTO = Omit<UsuarioResponseDTO, 'id'> & { password: string }
export type UsuarioUpdateRequestDTO = Omit<UsuarioResponseDTO, 'id'> & { password?: string }
