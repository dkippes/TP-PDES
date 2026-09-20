import type { View } from '../types'

type NavbarProps = {
  onNavigate: (view: View) => void
}

export function Navbar({ onNavigate }: NavbarProps) {
  return (
    <header className="border-b border-slate-200 bg-white">
      <nav className="mx-auto flex max-w-5xl items-center justify-between px-5 py-4" aria-label="Navegación principal">
        <button className="text-xl font-bold text-sky-700" type="button" onClick={() => onNavigate('home')}>AterrizAR</button>
        <div className="flex items-center gap-4 text-sm font-medium">
          <button className="text-slate-600 hover:text-sky-700" type="button" onClick={() => onNavigate('home')}>Inicio</button>
          <button className="text-slate-600 hover:text-sky-700" type="button" onClick={() => onNavigate('login')}>Iniciar sesión</button>
          <button className="rounded-md bg-sky-700 px-3 py-2 text-white hover:bg-sky-800" type="button" onClick={() => onNavigate('register')}>Registrarse</button>
        </div>
      </nav>
    </header>
  )
}
