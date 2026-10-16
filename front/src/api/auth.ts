import { readJson, sendJson } from './http'

export { ApiError as AuthError } from './http'

export type UsuarioAutenticado = {
  id: number
  nombre: string
  apellido: string
  correo: string
  direccion: string
  role: string
}

export type SesionAutenticada = {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: UsuarioAutenticado
}

type Registro = {
  nombre: string
  apellido: string
  correo: string
  direccion: string
  password: string
}

async function post<T>(path: string, body?: object): Promise<T> {
  return readJson<T>(await sendJson('POST', `auth/${path}`, body))
}

export const registrarUsuario = (datos: Registro) => post<UsuarioAutenticado>('register', datos)
export const iniciarSesion = (correo: string, password: string) => post<SesionAutenticada>('login', { correo, password })
export const renovarSesion = () => post<SesionAutenticada>('refresh')
export const cerrarSesion = () => post<void>('logout')
