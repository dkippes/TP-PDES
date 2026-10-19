import { useCallback, useState } from 'react'
import type { SesionAutenticada } from './api/auth'
import { requestJson } from './api/http'
import { createPaquetesApi } from './api/paquetes'
import { Navbar } from './components/Navbar'
import { AppLayout } from './components/layout/AppLayout'
import { useSession } from './hooks/useSession'
import { HomePage } from './pages/HomePage'
import { AdminHomePage } from './pages/AdminHomePage'
import { CompradorPage } from './pages/CompradorPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import type { View } from './types'
import type { UsuarioResponseDTO } from './types/admin'

const paquetesApi = createPaquetesApi(requestJson)

function App() {
  const [view, setView] = useState<View>('home')
  const [message, setMessage] = useState('')
  const handleSessionEnded = useCallback((notice: string) => {
    setMessage(notice)
    setView('login')
  }, [])
  const { session, restoring, loggingOut, acceptSession, renewSession, expireSession, updateCurrentUser, logout } = useSession({
    onMessage: setMessage, onSessionEnded: handleSessionEnded,
  })

  const handleLoggedIn = (authenticated: SesionAutenticada) => {
    acceptSession(authenticated)
    setMessage('')
    setView('home')
  }
  const handleRegistered = () => {
    setMessage('Cuenta creada. Iniciá sesión para continuar.')
    setView('login')
  }
  const handleLogout = async () => {
    if (await logout()) setView('home')
  }
  const handleCurrentUserUpdated = useCallback((usuario: UsuarioResponseDTO, passwordChanged: boolean) => {
    updateCurrentUser({
      id: usuario.id, nombre: usuario.nombre, apellido: usuario.apellido,
      correo: usuario.correo, direccion: usuario.direccion, role: usuario.rol,
    }, passwordChanged)
  }, [updateCurrentUser])
  const comprador = session?.user.role === 'COMPRADOR'
  const currentView = (view === 'admin' && session?.user.role !== 'ADMINISTRADOR') || (view === 'favoritos' && !comprador) ? 'home' : view

  return <AppLayout message={message} restoring={restoring} navigation={
    <Navbar view={currentView} onNavigate={setView} user={session?.user ?? null} onLogout={handleLogout} busy={restoring || loggingOut} />
  }>
    {currentView === 'home' && !comprador && <HomePage api={paquetesApi} isAuthenticated={session !== null} onRegister={() => setView('register')} onLogin={() => setView('login')} />}
    {(currentView === 'home' || currentView === 'favoritos') && comprador && session && <CompradorPage key={session.user.id} session={session} paquetesApi={paquetesApi} view={currentView} onHome={() => setView('home')} onSessionRenewed={renewSession} onSessionExpired={expireSession} />}
    {currentView === 'admin' && session?.user.role === 'ADMINISTRADOR' && <AdminHomePage session={session} onSessionRenewed={renewSession} onSessionExpired={expireSession} onCurrentUserUpdated={handleCurrentUserUpdated} />}
    {currentView === 'login' && <LoginPage onLoggedIn={handleLoggedIn} onRegister={() => setView('register')} />}
    {currentView === 'register' && <RegisterPage onRegistered={handleRegistered} onLogin={() => setView('login')} />}
  </AppLayout>
}

export default App
