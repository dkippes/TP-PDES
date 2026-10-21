import { useCallback, useState } from 'react'
import type { SesionAutenticada } from './api/auth'
import { requestJson } from './api/http'
import { createPaquetesApi } from './api/paquetes'
import { Navbar } from './components/Navbar'
import { AppLayout } from './components/layout/AppLayout'
import { useSession } from './hooks/useSession'
import { useNavigation } from './hooks/useNavigation'
import { HomePage } from './pages/HomePage'
import { AdminHomePage } from './pages/AdminHomePage'
import { CompradorPage } from './pages/CompradorPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { ResultadosPage } from './pages/ResultadosPage'
import { PaquetePage } from './pages/PaquetePage'
import type { View } from './types'
import type { UsuarioResponseDTO } from './types/admin'

const paquetesApi = createPaquetesApi(requestJson)
const vistasComprador = ['home', 'favoritos', 'compras', 'resultados', 'paquete'] as const
const esVistaComprador = (view: View): view is typeof vistasComprador[number] => (vistasComprador as readonly View[]).includes(view)

function App() {
  const { view, busqueda, paqueteId, navigate: setView, search: handleSearch, changeSearch: setBusqueda, openPackage, backToList } = useNavigation()
  const [message, setMessage] = useState('')
  const handleSessionEnded = useCallback((notice: string) => {
    setMessage(notice)
    setView('login')
  }, [setView])
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
  const currentView = (view === 'admin' && session?.user.role !== 'ADMINISTRADOR') || ((view === 'favoritos' || view === 'compras') && !comprador) ? 'home' : view

  return <AppLayout message={message} restoring={restoring} navigation={
    <Navbar view={currentView} onNavigate={setView} user={session?.user ?? null} onLogout={handleLogout} busy={restoring || loggingOut} />
  }>
    {comprador && session && esVistaComprador(currentView) && <CompradorPage key={session.user.id} session={session} paquetesApi={paquetesApi} view={currentView} paqueteId={paqueteId} onVerDetalle={openPackage} onVolverListado={backToList} busqueda={busqueda} onBusquedaChange={setBusqueda} onSearch={handleSearch} onHome={() => setView('home')} onCompras={() => setView('compras')} onSessionRenewed={renewSession} onSessionExpired={expireSession} />}
    {!comprador && currentView === 'home' && <HomePage api={paquetesApi} onVerDetalle={openPackage} onSearch={handleSearch} invitacion={session ? undefined : { onRegister: () => setView('register'), onLogin: () => setView('login') }} />}
    {!comprador && currentView === 'resultados' && <ResultadosPage api={paquetesApi} onVerDetalle={openPackage} filtros={busqueda} onFiltrosChange={setBusqueda} onBack={() => setView('home')} />}
    {!comprador && currentView === 'paquete' && <PaquetePage key={paqueteId} api={paquetesApi} paqueteId={paqueteId} onBack={backToList} />}
    {currentView === 'admin' && session?.user.role === 'ADMINISTRADOR' && <AdminHomePage session={session} onSessionRenewed={renewSession} onSessionExpired={expireSession} onCurrentUserUpdated={handleCurrentUserUpdated} />}
    {currentView === 'login' && <LoginPage onLoggedIn={handleLoggedIn} onRegister={() => setView('register')} />}
    {currentView === 'register' && <RegisterPage onRegistered={handleRegistered} onLogin={() => setView('login')} />}
  </AppLayout>
}

export default App
