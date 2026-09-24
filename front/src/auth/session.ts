import type { UsuarioAutenticado } from '../api/auth'

const SESSION_KEY = 'aterrizar.session'

export function readSession(): UsuarioAutenticado | null {
  const savedSession = localStorage.getItem(SESSION_KEY)
  if (!savedSession) return null

  try {
    const user = JSON.parse(savedSession) as UsuarioAutenticado
    return typeof user.id === 'number' && typeof user.nombre === 'string' && typeof user.correo === 'string'
      ? user
      : null
  } catch {
    localStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function saveSession(user: UsuarioAutenticado) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}
