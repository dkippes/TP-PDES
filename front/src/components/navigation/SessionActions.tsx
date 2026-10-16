import type { UsuarioAutenticado } from '../../api/auth'

type SessionActionsProps = {
  user: UsuarioAutenticado | null
  busy: boolean
  onLogin: () => void
  onRegister: () => void
  onLogout: () => void
}

export function SessionActions({ user, busy, onLogin, onRegister, onLogout }: SessionActionsProps) {
  if (user) return <>
    <span className="text-slate-600">Hola, {user.nombre}</span>
    <button className="rounded-md bg-slate-700 px-3 py-2 text-white hover:bg-slate-800 disabled:opacity-50" type="button" onClick={onLogout} disabled={busy}>Cerrar sesión</button>
  </>

  return <>
    <button className="text-slate-600 hover:text-sky-700" type="button" onClick={onLogin} disabled={busy}>Iniciar sesión</button>
    <button className="rounded-md bg-sky-700 px-3 py-2 text-white hover:bg-sky-800" type="button" onClick={onRegister} disabled={busy}>Registrarse</button>
  </>
}
