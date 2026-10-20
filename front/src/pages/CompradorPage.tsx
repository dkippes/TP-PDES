import { useMemo } from 'react'
import type { SesionAutenticada } from '../api/auth'
import { createAuthenticatedRequest, type SessionEvents } from '../api/authenticated'
import { createComprasApi } from '../api/compras'
import { createFavoritosApi } from '../api/favoritos'
import type { PaquetesApi } from '../api/paquetes'
import { CompraModal } from '../components/compras/CompraModal'
import { useCompra } from '../hooks/useCompra'
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

/** Buyer views share one favorites state and one purchase dialog, so both survive navigating between them. */
export function CompradorPage({ session, paquetesApi, view, busqueda, onBusquedaChange, onSearch, onHome, onSessionRenewed, onSessionExpired }: CompradorPageProps) {
  const request = useMemo(() => createAuthenticatedRequest(session.accessToken, { onSessionRenewed, onSessionExpired }),
    [session.accessToken, onSessionRenewed, onSessionExpired])
  const favoritosApi = useMemo(() => createFavoritosApi(request), [request])
  const comprasApi = useMemo(() => createComprasApi(request), [request])
  const { paquetes, controls } = useFavoritos(favoritosApi)
  const compra = useCompra(comprasApi)

  const vista = () => {
    if (view === 'favoritos') return <FavoritosPage paquetes={paquetes} favoritos={controls} onHome={onHome} onComprar={compra.abrir} />
    if (view === 'resultados') return <ResultadosPage api={paquetesApi} filtros={busqueda} onFiltrosChange={onBusquedaChange} onBack={onHome} favoritos={controls} onComprar={compra.abrir} />
    return <HomePage api={paquetesApi} onSearch={onSearch} favoritos={controls} onComprar={compra.abrir} />
  }

  return <>
    {vista()}
    {compra.paquete && <CompraModal paquete={compra.paquete} compra={compra.compra} busy={compra.busy} error={compra.error}
      onConfirm={() => void compra.confirmar()} onClose={compra.cerrar} />}
  </>
}
