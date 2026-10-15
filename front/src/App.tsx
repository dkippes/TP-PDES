import { useCallback, useEffect, useState } from 'react'
import { AuthError, cerrarSesion, type SesionAutenticada } from './api/auth'
import { requestJson } from './api/http'
import { createPaquetesApi } from './api/paquetes'
import { restorePendingRefresh, restoreSession } from './auth/session'
import { Navbar } from './components/Navbar'
import { HomePage } from './pages/HomePage'
import { AdminHomePage } from './pages/AdminHomePage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import type { View } from './types'
import type { UsuarioResponseDTO } from './types/admin'

const paquetesApi = createPaquetesApi(requestJson)

function App() {
  const [view, setView] = useState<View>('home')
  const [session, setSession] = useState<SesionAutenticada | null>(null)
  const [restoring, setRestoring] = useState(true)
  const [loggingOut, setLoggingOut] = useState(false)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let cancelled = false
    // Discard the old cosmetic session; only the server can restore authentication.
    localStorage.removeItem('aterrizar.session')
    restoreSession()
      .then((restored) => { if (!cancelled) setSession(restored) })
      .catch((error: unknown) => {
        if (!cancelled && !(error instanceof AuthError && error.status === 401)) {
          setMessage('No se pudo recuperar la sesión. Intentá iniciar sesión nuevamente.')
        }
      })
      .finally(() => { if (!cancelled) setRestoring(false) })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    if (!session || loggingOut) return
    let cancelled = false
    const timeout = window.setTimeout(() => {
      restoreSession().then((renewed) => {
        if (!cancelled) setSession(renewed)
      }).catch(() => {
        if (!cancelled) {
          setSession(null)
          setMessage('Tu sesión terminó. Iniciá sesión nuevamente.')
          setView('login')
        }
      })
    }, Math.max(1000, (session.expiresIn - 30) * 1000))
    return () => {
      cancelled = true
      window.clearTimeout(timeout)
    }
  }, [session, loggingOut])

  const handleLoggedIn = (loggedInSession: SesionAutenticada) => {
    setSession(loggedInSession)
    setMessage('')
    setView('home')
  }

  const handleRegistered = () => {
    setMessage('Cuenta creada. Iniciá sesión para continuar.')
    setView('login')
  }

  const handleSessionExpired = useCallback(() => {
    setSession(null)
    setView('login')
    setMessage('Tu sesión terminó. Iniciá sesión nuevamente.')
  }, [])

  const handleSessionRenewed = useCallback((renewed: SesionAutenticada) => {
    setSession((current) => current ? renewed : null)
  }, [])

  const handleCurrentUserUpdated = useCallback((usuario: UsuarioResponseDTO, passwordChanged: boolean) => {
    if (passwordChanged) {
      setSession(null)
      setView('login')
      setMessage('Contraseña actualizada. Iniciá sesión nuevamente.')
      return
    }
    setSession((current) => current && current.user.id === usuario.id ? {
      ...current,
      user: {
        id: usuario.id, nombre: usuario.nombre, apellido: usuario.apellido,
        correo: usuario.correo, direccion: usuario.direccion, role: usuario.rol,
      },
    } : current)
  }, [])

  const handleLogout = async () => {
    setLoggingOut(true)
    setMessage('')
    try {
      // Wait for a rotating refresh before revoking its replacement cookie.
      await restorePendingRefresh()
      await cerrarSesion()
      setSession(null)
      setView('home')
    } catch {
      setMessage('No se pudo cerrar la sesión. Intentá nuevamente.')
    } finally {
      setLoggingOut(false)
    }
  }

  const currentView = view === 'admin' && session?.user.role !== 'ADMINISTRADOR' ? 'home' : view

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800">
      <Navbar view={currentView} onNavigate={setView} user={session?.user ?? null} onLogout={handleLogout} busy={restoring || loggingOut} />
      <main className="mx-auto max-w-6xl px-5 py-10 sm:py-14">
        {message && <p className="mb-4 text-sm text-slate-700" role="status">{message}</p>}
        {restoring ? <p role="status">Recuperando sesión...</p> : <>
          {currentView === 'home' && <HomePage api={paquetesApi} />}
          {currentView === 'admin' && session?.user.role === 'ADMINISTRADOR' && <AdminHomePage session={session} onSessionRenewed={handleSessionRenewed} onSessionExpired={handleSessionExpired} onCurrentUserUpdated={handleCurrentUserUpdated} />}
          {currentView === 'login' && <LoginPage onLoggedIn={handleLoggedIn} onRegister={() => setView('register')} />}
          {currentView === 'register' && <RegisterPage onRegistered={handleRegistered} onLogin={() => setView('login')} />}
        </>}
      </main>
    </div>
  )
}

export default App
