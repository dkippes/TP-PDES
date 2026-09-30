import { useEffect, useState } from 'react'
import { AuthError, cerrarSesion, type SesionAutenticada } from './api/auth'
import { restorePendingRefresh, restoreSession } from './auth/session'
import { Navbar } from './components/Navbar'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import type { View } from './types'

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

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800">
      <Navbar onNavigate={setView} user={session?.user ?? null} onLogout={handleLogout} busy={restoring || loggingOut} />
      <main className="mx-auto max-w-5xl px-5 py-16">
        {message && <p className="mb-4 text-sm text-slate-700" role="status">{message}</p>}
        {restoring ? <p role="status">Recuperando sesión...</p> : <>
          {view === 'home' && <HomePage authenticated={session !== null} onRegister={() => setView('register')} />}
          {view === 'login' && <LoginPage onLoggedIn={handleLoggedIn} onRegister={() => setView('register')} />}
          {view === 'register' && <RegisterPage onRegistered={handleRegistered} onLogin={() => setView('login')} />}
        </>}
      </main>
    </div>
  )
}

export default App
