import { useEffect, useMemo } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'
import type { SesionAutenticada } from '../api/auth'
import { createAuthenticatedRequest, type SessionEvents } from '../api/authenticated'
import { createComprasApi } from '../api/compras'
import { createFavoritosApi } from '../api/favoritos'
import { createValoracionesApi } from '../api/valoraciones'
import type { PaquetesApi } from '../api/paquetes'
import { CompraModal } from '../components/compras/CompraModal'
import { useCompra } from '../hooks/useCompra'
import { useCompras } from '../hooks/useCompras'
import { useDisponibilidades } from '../hooks/useDisponibilidades'
import { useFavoritos } from '../hooks/useFavoritos'
import type { CompradorContext } from '../routes/compradorContext'
import { rutas } from '../routes/rutas'
import type { CompraControls } from '../types/compra'

type CompradorPageProps = SessionEvents & {
  session: SesionAutenticada
  paquetesApi: PaquetesApi
}

/** Layout route for buyers: catalog, detail, favorites and purchases share its state and one purchase dialog, so they survive navigating between routes. */
export function CompradorPage({ session, paquetesApi, onSessionRenewed, onSessionExpired }: CompradorPageProps) {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const request = useMemo(() => createAuthenticatedRequest(session.accessToken, { onSessionRenewed, onSessionExpired }),
    [session.accessToken, onSessionRenewed, onSessionExpired])
  const favoritosApi = useMemo(() => createFavoritosApi(request), [request])
  const comprasApi = useMemo(() => createComprasApi(request), [request])
  const valoracionesApi = useMemo(() => createValoracionesApi(request), [request])
  const { paquetes, controls, actualizar } = useFavoritos(favoritosApi)
  const historial = useCompras(comprasApi)
  const { disponibilidades, refrescar, limpiar } = useDisponibilidades(paquetesApi)
  // The post-purchase availability lasts until the route changes, when the new page reads it from the server.
  useEffect(() => { limpiar() }, [pathname, limpiar])
  const compra = useCompra(comprasApi, (confirmada) => {
    historial.registrar(confirmada)
    void refrescar(confirmada.paquete.id).then((paquete) => { if (paquete) actualizar(paquete) })
  })
  const compraControls: CompraControls = { comprados: historial.comprados, disponibilidades, comprar: compra.abrir }
  const context: CompradorContext = { favoritos: controls, paquetesFavoritos: paquetes, compra: compraControls, historial, valoracionesApi }

  return <>
    <Outlet context={context} />
    {compra.paquete && <CompraModal paquete={compra.paquete} compra={compra.compra} busy={compra.busy} error={compra.error}
      onConfirm={() => void compra.confirmar()} onClose={compra.cerrar}
      onVerCompras={() => { compra.cerrar(); void navigate(rutas.compras) }} />}
  </>
}
