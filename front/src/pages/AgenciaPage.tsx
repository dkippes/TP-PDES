import { useCallback, useMemo, useState } from 'react'
import { createAgenciaApi } from '../api/agencia'
import { createAuthenticatedRequest, type SessionEvents } from '../api/authenticated'
import type { SesionAutenticada } from '../api/auth'
import { AgenciaSections, type AgenciaSection } from '../components/agencia/AgenciaSections'
import { PaquetesAgenciaPanel } from '../components/agencia/PaquetesAgenciaPanel'
import { VentasPanel } from '../components/agencia/VentasPanel'
import { secondaryClass } from '../components/admin/controls'
import { useRemoteData } from '../hooks/useRemoteData'
import type { UsuarioResponseDTO } from '../types/admin'

type Props = SessionEvents & { session: SesionAutenticada }

/** Workspace of an AGENTE: packages and sales of the agency its account belongs to. */
export function AgenciaPage({ session, onSessionRenewed, onSessionExpired }: Readonly<Props>) {
  const [section, setSection] = useState<AgenciaSection>('paquetes')
  const api = useMemo(() => createAgenciaApi(createAuthenticatedRequest(session.accessToken, { onSessionRenewed, onSessionExpired })),
    [session.accessToken, onSessionRenewed, onSessionExpired])
  const loadPerfil = useCallback(() => api.perfil(), [api])
  const perfil = useRemoteData<UsuarioResponseDTO | null>(loadPerfil, null)
  const agenciaId = perfil.data?.agenciaId ?? null

  return <div className="space-y-6">
    <section className="rounded-2xl bg-slate-900 p-7 text-white sm:p-9">
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-300">Panel de agencia</p>
      <h1 className="mt-3 text-3xl font-bold tracking-tight sm:text-4xl">Mi agencia</h1>
      <p className="mt-3 max-w-xl text-sm leading-6 text-slate-300">Gestioná los paquetes que ofrece tu agencia y consultá tus ventas y clientes.</p>
    </section>
    {perfil.loading && <p role="status" className="py-10 text-center text-slate-500">Cargando tu agencia...</p>}
    {perfil.error && <div className="py-6"><p role="alert" className="mb-4 text-sm text-red-700">{perfil.error}</p><button type="button" className={secondaryClass} onClick={perfil.reload}>Reintentar</button></div>}
    {!perfil.loading && !perfil.error && (agenciaId === null
      ? <p role="alert" className="rounded-lg bg-amber-50 p-4 text-sm text-amber-800">Tu cuenta no está asociada a ninguna agencia.</p>
      : <>
        <AgenciaSections section={section} onSelect={setSection} />
        {section === 'paquetes' ? <PaquetesAgenciaPanel api={api} agenciaId={agenciaId} /> : <VentasPanel api={api} />}
      </>)}
  </div>
}
