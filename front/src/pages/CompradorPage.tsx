import { useMemo } from 'react'
import type { SesionAutenticada } from '../api/auth'
import { createAuthenticatedRequest, type SessionEvents } from '../api/authenticated'
import { createFavoritosApi } from '../api/favoritos'
import type { PaquetesApi } from '../api/paquetes'
import { useFavoritos } from '../hooks/useFavoritos'
import type { PaqueteBusquedaDTO } from '../types/paquete'
import { FavoritosPage } from './FavoritosPage'
import { HomePage } from './HomePage'
import { ResultadosPage } from './ResultadosPage'

type CompradorPageProps = SessionEvents & {
  session: SesionAutenticada
  paquetesApi: PaquetesApi
  view: 'home' | 'favoritos' | 'resultados'
  busqueda: PaqueteBusquedaDTO
  onBusquedaChange: (filtros: PaqueteBusquedaDTO) => void
  onSearch: (filtros: PaqueteBusquedaDTO) => void
  onHome: () => void
}

/** Buyer views share one favorites state, so it is loaded once while navigating between them. */
export function CompradorPage({ session, paquetesApi, view, busqueda, onBusquedaChange, onSearch, onHome, onSessionRenewed, onSessionExpired }: CompradorPageProps) {
  const api = useMemo(() => createFavoritosApi(createAuthenticatedRequest(session.accessToken, { onSessionRenewed, onSessionExpired })),
    [session.accessToken, onSessionRenewed, onSessionExpired])
  const { paquetes, controls } = useFavoritos(api)

  if (view === 'favoritos') return <FavoritosPage paquetes={paquetes} favoritos={controls} onHome={onHome} />
  if (view === 'resultados') return <ResultadosPage api={paquetesApi} filtros={busqueda} onFiltrosChange={onBusquedaChange} onBack={onHome} favoritos={controls} />
  return <HomePage api={paquetesApi} onSearch={onSearch} favoritos={controls} />
}
