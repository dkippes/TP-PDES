import { useCallback, useEffect, useState } from 'react'
import { AuthError, cerrarSesion, type SesionAutenticada, type UsuarioAutenticado } from '../api/auth'
import { restorePendingRefresh, restoreSession } from '../auth/session'

type SessionCallbacks = {
  onMessage: (message: string) => void
  onSessionEnded: (message: string) => void
}

export function useSession({ onMessage, onSessionEnded }: SessionCallbacks) {
  const [session, setSession] = useState<SesionAutenticada | null>(null)
  const [restoring, setRestoring] = useState(true)
  const [loggingOut, setLoggingOut] = useState(false)

  useEffect(() => {
    let cancelled = false
    // Only the server can restore authentication; discard the old cosmetic session.
    localStorage.removeItem('aterrizar.session')
    restoreSession()
      .then((restored) => { if (!cancelled) setSession(restored) })
      .catch((error: unknown) => {
        if (!cancelled && !(error instanceof AuthError && error.status === 401)) {
          onMessage('No se pudo recuperar la sesión. Intentá iniciar sesión nuevamente.')
        }
      })
      .finally(() => { if (!cancelled) setRestoring(false) })
    return () => { cancelled = true }
  }, [onMessage])

  const expireSession = useCallback(() => {
    setSession(null)
    onSessionEnded('Tu sesión terminó. Iniciá sesión nuevamente.')
  }, [onSessionEnded])

  useEffect(() => {
    if (!session || loggingOut) return
    let cancelled = false
    const timeout = window.setTimeout(() => {
      restoreSession().then((renewed) => {
        if (!cancelled) setSession(renewed)
      }).catch(() => { if (!cancelled) expireSession() })
    }, Math.max(1000, (session.expiresIn - 30) * 1000))
    return () => {
      cancelled = true
      window.clearTimeout(timeout)
    }
  }, [session, loggingOut, expireSession])

  const acceptSession = useCallback((authenticated: SesionAutenticada) => {
    setSession(authenticated)
  }, [])

  const renewSession = useCallback((renewed: SesionAutenticada) => {
    setSession((current) => current ? renewed : null)
  }, [])

  const updateCurrentUser = useCallback((user: UsuarioAutenticado, passwordChanged: boolean) => {
    if (passwordChanged) {
      setSession(null)
      onSessionEnded('Contraseña actualizada. Iniciá sesión nuevamente.')
      return
    }
    setSession((current) => current && current.user.id === user.id ? {
      ...current,
      user,
    } : current)
  }, [onSessionEnded])

  const logout = async (): Promise<boolean> => {
    setLoggingOut(true)
    onMessage('')
    try {
      // Revoke the replacement cookie if a rotating refresh is already in flight.
      await restorePendingRefresh()
      await cerrarSesion()
      setSession(null)
      return true
    } catch {
      onMessage('No se pudo cerrar la sesión. Intentá nuevamente.')
      return false
    } finally {
      setLoggingOut(false)
    }
  }

  return { session, restoring, loggingOut, acceptSession, renewSession, expireSession, updateCurrentUser, logout }
}
