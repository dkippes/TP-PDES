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

export class AuthError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

const apiUrl = (import.meta.env?.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '')

async function post<T>(path: string, body?: object): Promise<T> {
  const response = await fetch(`${apiUrl}/api/auth/${path}`, {
    method: 'POST',
    credentials: 'include',
    ...(body === undefined ? {} : {
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null) as {
      message?: string
      fieldErrors?: Record<string, string>
    } | null
    const fields = Object.entries(error?.fieldErrors ?? {}).map(([field, message]) => `${field}: ${message}`)
    throw new AuthError(response.status, fields.length > 0
      ? fields.join('. ')
      : error?.message ?? 'No se pudo completar la operación.')
  }
  // Logout has a successful response with no JSON body.
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export const registrarUsuario = (datos: Registro) => post<UsuarioAutenticado>('register', datos)
export const iniciarSesion = (correo: string, password: string) => post<SesionAutenticada>('login', { correo, password })
export const renovarSesion = () => post<SesionAutenticada>('refresh')
export const cerrarSesion = () => post<void>('logout')
