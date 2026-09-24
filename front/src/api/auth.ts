export type UsuarioAutenticado = { id: number; nombre: string; correo: string }

type Registro = {
  nombre: string
  apellido: string
  correo: string
  direccion: string
  password: string
}

const apiUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

async function post<T>(path: string, body: object): Promise<T> {
  const response = await fetch(`${apiUrl}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null) as { detail?: string } | null
    throw new Error(error?.detail ?? 'No se pudo completar la operación.')
  }
  return response.json() as Promise<T>
}

export const registrarUsuario = (datos: Registro) => post<UsuarioAutenticado>('/api/auth/registro', datos)
export const iniciarSesion = (correo: string, password: string) => post<UsuarioAutenticado>('/api/auth/login', { correo, password })
