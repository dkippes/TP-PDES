import type { View } from '../types'
import type { UsuarioAutenticado } from '../api/auth'
import { SessionActions } from './navigation/SessionActions'

type NavbarProps = {
  view: View
  onNavigate: (view: View) => void
  user: UsuarioAutenticado | null
  onLogout: () => void
  busy: boolean
}

export function Navbar({ view, onNavigate, user, onLogout, busy }: NavbarProps) {
  return (
    <header className="border-b border-slate-200 bg-white">
      <nav className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-5 py-4" aria-label="Navegación principal">
        <button className="text-xl font-bold text-sky-700" type="button" onClick={() => onNavigate('home')}>AterrizAR</button>
        <div className="flex flex-wrap items-center gap-4 text-sm font-medium">
          <button className="text-slate-600 hover:text-sky-700 aria-[current=page]:font-bold aria-[current=page]:text-sky-700" type="button" aria-current={view === 'home' ? 'page' : undefined} onClick={() => onNavigate('home')}>Inicio</button>
          {user?.role === 'ADMINISTRADOR' && <button className="text-slate-600 hover:text-sky-700 aria-[current=page]:font-bold aria-[current=page]:text-sky-700" type="button" aria-current={view === 'admin' ? 'page' : undefined} onClick={() => onNavigate('admin')} disabled={busy}>Administración</button>}
          <SessionActions user={user} busy={busy} onLogin={() => onNavigate('login')} onRegister={() => onNavigate('register')} onLogout={onLogout} />
        </div>
      </nav>
    </header>
  )
}
