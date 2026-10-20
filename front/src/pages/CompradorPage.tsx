import { useMemo } from 'react'
import type { SesionAutenticada } from '../api/auth'
import { createAuthenticatedRequest, type SessionEvents } from '../api/authenticated'
import { createFavoritosApi } from '../api/favoritos'
import type { PaquetesApi } from '../api/paquetes'
import { useFavoritos } from '../hooks/useFavoritos'
import { usePaquetesCatalogo } from '../hooks/usePaquetesCatalogo'
import { FavoritosStatus } from '../components/paquetes/FavoritosStatus'
import { PaquetesCatalog } from '../components/paquetes/PaquetesCatalog'
import { PaquetesSearchBar } from '../components/paquetes/PaquetesSearchBar'
import type { PaqueteBusquedaDTO } from '../types/paquete'
import { FavoritosPage } from './FavoritosPage'

type CompradorPageProps = SessionEvents & {
  session: SesionAutenticada
  paquetesApi: PaquetesApi
  view: 'home' | 'favoritos'
  onHome: () => void
  onSearch: (filtros: PaqueteBusquedaDTO) => void
}

export function CompradorPage({ session, paquetesApi, view, onHome, onSearch, onSessionRenewed, onSessionExpired }: CompradorPageProps) {
  const api = useMemo(() => createFavoritosApi(createAuthenticatedRequest(session.accessToken, { onSessionRenewed, onSessionExpired })),
    [session.accessToken, onSessionRenewed, onSessionExpired])
  const { paquetes, controls } = useFavoritos(api)
  const catalogo = usePaquetesCatalogo(paquetesApi)

  if (view === 'favoritos') return <FavoritosPage paquetes={paquetes} favoritos={controls} onHome={onHome} />

  return <div className="space-y-6">
    <PaquetesSearchBar sugerencias={catalogo.sugerencias} onSearch={onSearch} />
    <FavoritosStatus favoritos={controls} />
    <PaquetesCatalog paquetes={catalogo.data} loading={catalogo.loading} error={catalogo.error} onRetry={catalogo.reload} favoritos={controls} />
  </div>
}
