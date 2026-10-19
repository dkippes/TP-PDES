import { useMemo } from 'react'
import type { SesionAutenticada } from '../api/auth'
import { createAuthenticatedRequest, type SessionEvents } from '../api/authenticated'
import { createFavoritosApi } from '../api/favoritos'
import type { PaquetesApi } from '../api/paquetes'
import { useFavoritos } from '../hooks/useFavoritos'
import { FavoritosStatus } from '../components/paquetes/FavoritosStatus'
import { PaquetesCatalog } from '../components/paquetes/PaquetesCatalog'
import { FavoritosPage } from './FavoritosPage'

type CompradorPageProps = SessionEvents & {
  session: SesionAutenticada
  paquetesApi: PaquetesApi
  view: 'home' | 'favoritos'
  onHome: () => void
}

export function CompradorPage({ session, paquetesApi, view, onHome, onSessionRenewed, onSessionExpired }: CompradorPageProps) {
  const api = useMemo(() => createFavoritosApi(createAuthenticatedRequest(session.accessToken, { onSessionRenewed, onSessionExpired })),
    [session.accessToken, onSessionRenewed, onSessionExpired])
  const { paquetes, controls } = useFavoritos(api)

  if (view === 'favoritos') return <FavoritosPage paquetes={paquetes} favoritos={controls} onHome={onHome} />

  return <div className="space-y-6">
    <FavoritosStatus favoritos={controls} />
    <PaquetesCatalog api={paquetesApi} favoritos={controls} />
  </div>
}
