import { useMemo, useState } from 'react'
import { createAdminApi } from '../api/admin'
import { createAuthenticatedRequest } from '../api/authenticated'
import type { SesionAutenticada } from '../api/auth'
import { AgenciasPanel } from '../components/admin/AgenciasPanel'
import { UsuariosPanel } from '../components/admin/UsuariosPanel'
import type { UsuarioResponseDTO } from '../types/admin'

type Props = {
  session: SesionAutenticada
  onSessionRenewed: (session: SesionAutenticada) => void
  onSessionExpired: () => void
  onCurrentUserUpdated: (usuario: UsuarioResponseDTO, passwordChanged: boolean) => void
}

export function AdminHomePage({ session, onSessionRenewed, onSessionExpired, onCurrentUserUpdated }: Props) {
  const [section, setSection] = useState<'usuarios' | 'agencias'>('usuarios')
  const api = useMemo(() => createAdminApi(createAuthenticatedRequest(session.accessToken, { onSessionRenewed, onSessionExpired })),
    [session.accessToken, onSessionRenewed, onSessionExpired])

  return <div className="space-y-6">
    <section className="rounded-2xl bg-slate-900 p-7 text-white sm:p-9">
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-300">Panel de administrador</p>
      <h1 className="mt-3 text-3xl font-bold tracking-tight sm:text-4xl">Administración</h1>
      <p className="mt-3 max-w-xl text-sm leading-6 text-slate-300">Gestioná los usuarios y las agencias de AterrizAR desde un solo lugar.</p>
    </section>
    <nav aria-label="Secciones de administración" className="flex gap-2">
      {(['usuarios', 'agencias'] as const).map((item) => <button key={item} type="button" aria-pressed={section === item} onClick={() => setSection(item)}
        className={`rounded-lg px-5 py-3 text-sm font-semibold transition ${section === item ? 'bg-sky-700 text-white' : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-100'}`}>
        {item === 'usuarios' ? 'Usuarios' : 'Agencias'}
      </button>)}
    </nav>
    {section === 'usuarios'
      ? <UsuariosPanel api={api.usuarios} agenciasApi={api.agencias} currentUserId={session.user.id} onCurrentUserUpdated={onCurrentUserUpdated} />
      : <AgenciasPanel api={api.agencias} />}
  </div>
}
