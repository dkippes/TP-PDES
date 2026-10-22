import { useCallback, useEffect, useRef, useState } from 'react'
import { Navigate, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router'
import type { SesionAutenticada } from './api/auth'
import { requestJson } from './api/http'
import { createPaquetesApi } from './api/paquetes'
import { Navbar } from './components/Navbar'
import { AppLayout } from './components/layout/AppLayout'
import { useSession } from './hooks/useSession'
import { AdminHomePage } from './pages/AdminHomePage'
import { CompradorPage } from './pages/CompradorPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { ComprasRoute, FavoritosRoute, InicioRoute, PaqueteRoute, ResultadosRoute } from './routes/CatalogoRoutes'
import { RequireRole } from './routes/RequireRole'
import { esRutaDeVisitante, rutas, type DestinoState } from './routes/rutas'
import { SoloVisitantes } from './routes/SoloVisitantes'
import type { UsuarioResponseDTO } from './types/admin'

const paquetesApi = createPaquetesApi(requestJson)

function App() {
  const navigate = useNavigate()
  const location = useLocation()
  // useNavigate changes identity on every navigation; session callbacks must stay stable so API clients are not recreated.
  const navigateRef = useRef(navigate)
  useEffect(() => { navigateRef.current = navigate })
  useEffect(() => { window.scrollTo(0, 0) }, [location.key])
  const [message, setMessage] = useState('')
  const handleSessionEnded = useCallback((notice: string) => {
    setMessage(notice)
    // Concurrent requests may report the same expiry more than once; only the first one keeps the destination.
    if (esRutaDeVisitante(window.location.pathname)) return
    navigateRef.current(rutas.login, { state: { from: window.location.pathname } satisfies DestinoState })
  }, [])
  const { session, restoring, loggingOut, acceptSession, renewSession, expireSession, updateCurrentUser, logout } = useSession({
    onMessage: setMessage, onSessionEnded: handleSessionEnded,
  })

  // SoloVisitantes sends the new session to the page that required it, or to Inicio.
  const handleLoggedIn = (authenticated: SesionAutenticada) => {
    acceptSession(authenticated)
    setMessage('')
  }
  // Login and registration forward the location state so the visitor keeps its destination.
  const handleRegistered = () => {
    setMessage('Cuenta creada. Iniciá sesión para continuar.')
    navigate(rutas.login, { state: location.state })
  }
  const handleLogout = () => logout(() => navigate(rutas.inicio))
  const handleCurrentUserUpdated = useCallback((usuario: UsuarioResponseDTO, passwordChanged: boolean) => {
    updateCurrentUser({
      id: usuario.id, nombre: usuario.nombre, apellido: usuario.apellido,
      correo: usuario.correo, direccion: usuario.direccion, role: usuario.rol,
    }, passwordChanged)
  }, [updateCurrentUser])
  const comprador = session?.user.role === 'COMPRADOR' ? session : null

  return <AppLayout message={message} restoring={restoring} navigation={
    <Navbar user={session?.user ?? null} onLogout={handleLogout} busy={restoring || loggingOut} />
  }>
    <Routes>
      {/* Every route shares the layout, so a buyer redirected by a guard keeps CompradorPage mounted. */}
      <Route element={comprador
        ? <CompradorPage key={comprador.user.id} session={comprador} paquetesApi={paquetesApi} onSessionRenewed={renewSession} onSessionExpired={expireSession} />
        : <Outlet />}>
        <Route index element={<InicioRoute api={paquetesApi} session={session} />} />
        <Route path={rutas.resultados} element={<ResultadosRoute api={paquetesApi} />} />
        <Route path="/paquetes/:id" element={<PaqueteRoute api={paquetesApi} />} />
        <Route path={rutas.favoritos} element={<RequireRole session={session} role="COMPRADOR">{() => <FavoritosRoute />}</RequireRole>} />
        <Route path={rutas.compras} element={<RequireRole session={session} role="COMPRADOR">{() => <ComprasRoute />}</RequireRole>} />
        <Route path={rutas.admin} element={<RequireRole session={session} role="ADMINISTRADOR">{(admin) =>
          <AdminHomePage session={admin} onSessionRenewed={renewSession} onSessionExpired={expireSession} onCurrentUserUpdated={handleCurrentUserUpdated} />
        }</RequireRole>} />
        <Route path={rutas.login} element={<SoloVisitantes session={session}>
          <LoginPage onLoggedIn={handleLoggedIn} onRegister={() => navigate(rutas.registro, { state: location.state })} />
        </SoloVisitantes>} />
        <Route path={rutas.registro} element={<SoloVisitantes session={session}>
          <RegisterPage onRegistered={handleRegistered} onLogin={() => navigate(rutas.login, { state: location.state })} />
        </SoloVisitantes>} />
        <Route path="*" element={<Navigate to={rutas.inicio} replace />} />
      </Route>
    </Routes>
  </AppLayout>
}

export default App
