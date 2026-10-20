import { useMemo } from 'react'
import type { SesionAutenticada } from '../api/auth'
import { createAuthenticatedRequest, type SessionEvents } from '../api/authenticated'
import { createComprasApi } from '../api/compras'
import { createFavoritosApi } from '../api/favoritos'
import type { PaquetesApi } from '../api/paquetes'
import { CompraModal } from '../components/compras/CompraModal'
import { useCompra } from '../hooks/useCompra'
import { useCompras } from '../hooks/useCompras'
import { useFavoritos } from '../hooks/useFavoritos'
import type { CompraControls } from '../types/compra'
import type { PaqueteBusquedaDTO } from '../types/paquete'
import { ComprasPage } from './ComprasPage'
import { FavoritosPage } from './FavoritosPage'
import { HomePage } from './HomePage'
import { ResultadosPage } from './ResultadosPage'

type CompradorPageProps = SessionEvents & {
  session: SesionAutenticada
  paquetesApi: PaquetesApi
  view: 'home' | 'favoritos' | 'compras' | 'resultados'
  busqueda: PaqueteBusquedaDTO
  onBusquedaChange: (filtros: PaqueteBusquedaDTO) => void
  onSearch: (filtros: PaqueteBusquedaDTO) => void
  onHome: () => void
}

/** Buyer views share favorites, purchases and one purchase dialog, so they survive navigating between views. */
export function CompradorPage({ session, paquetesApi, view, busqueda, onBusquedaChange, onSearch, onHome, onSessionRenewed, onSessionExpired }: CompradorPageProps) {
  const request = useMemo(() => createAuthenticatedRequest(session.accessToken, { onSessionRenewed, onSessionExpired }),
    [session.accessToken, onSessionRenewed, onSessionExpired])
  const favoritosApi = useMemo(() => createFavoritosApi(request), [request])
  const comprasApi = useMemo(() => createComprasApi(request), [request])
  const { paquetes, controls } = useFavoritos(favoritosApi)
  const historial = useCompras(comprasApi)
  const compra = useCompra(comprasApi, historial.registrar)
  const compraControls: CompraControls = { comprados: historial.comprados, comprar: compra.abrir }

  const vista = () => {
    if (view === 'favoritos') return <FavoritosPage paquetes={paquetes} favoritos={controls} compra={compraControls} onHome={onHome} />
    if (view === 'compras') return <ComprasPage compras={historial.compras} loading={historial.loading} error={historial.error} onRetry={historial.reload} onHome={onHome} />
    if (view === 'resultados') return <ResultadosPage api={paquetesApi} filtros={busqueda} onFiltrosChange={onBusquedaChange} onBack={onHome} favoritos={controls} compra={compraControls} />
    return <HomePage api={paquetesApi} onSearch={onSearch} favoritos={controls} compra={compraControls} />
  }

  return <>
    {vista()}
    {compra.paquete && <CompraModal paquete={compra.paquete} compra={compra.compra} busy={compra.busy} error={compra.error}
      onConfirm={() => void compra.confirmar()} onClose={compra.cerrar} />}
  </>
}
