import { useLocation, useNavigate } from 'react-router'
import type { UsuarioAutenticado } from '../api/auth'
import { rutas } from '../routes/rutas'
import { SessionActions } from './navigation/SessionActions'

type NavbarProps = {
  user: UsuarioAutenticado | null
  onLogout: () => void
  busy: boolean
}

const linkClass = 'text-slate-600 hover:text-sky-700 aria-[current=page]:font-bold aria-[current=page]:text-sky-700'

export function Navbar({ user, onLogout, busy }: Readonly<NavbarProps>) {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const current = (path: string) => pathname === path ? 'page' : undefined

  return (
    <header className="border-b border-slate-200 bg-white">
      <nav className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-5 py-4" aria-label="Navegación principal">
        <button className="text-xl font-bold text-sky-700" type="button" onClick={() => navigate(rutas.inicio)}>AterrizAR</button>
        <div className="flex flex-wrap items-center gap-4 text-sm font-medium">
          <button className={linkClass} type="button" aria-current={current(rutas.inicio)} onClick={() => navigate(rutas.inicio)}>Inicio</button>
          {user?.role === 'COMPRADOR' && <button className={linkClass} type="button" aria-current={current(rutas.favoritos)} onClick={() => navigate(rutas.favoritos)} disabled={busy}>Mis favoritos</button>}
          {user?.role === 'COMPRADOR' && <button className={linkClass} type="button" aria-current={current(rutas.compras)} onClick={() => navigate(rutas.compras)} disabled={busy}>Mis compras</button>}
          {user?.role === 'ADMINISTRADOR' && <button className={linkClass} type="button" aria-current={current(rutas.admin)} onClick={() => navigate(rutas.admin)} disabled={busy}>Administración</button>}
          <SessionActions user={user} busy={busy} onLogin={() => navigate(rutas.login)} onRegister={() => navigate(rutas.registro)} onLogout={onLogout} />
        </div>
      </nav>
    </header>
  )
}
