import { useNavigate, useParams } from 'react-router'
import type { SesionAutenticada } from '../api/auth'
import type { PaquetesApi } from '../api/paquetes'
import { useNavigation } from '../hooks/useNavigation'
import { ComprasPage } from '../pages/ComprasPage'
import { FavoritosPage } from '../pages/FavoritosPage'
import { HomePage } from '../pages/HomePage'
import { PaquetePage } from '../pages/PaquetePage'
import { ResultadosPage } from '../pages/ResultadosPage'
import { useCompradorContext } from './compradorContext'
import { rutas } from './rutas'

type CatalogoRouteProps = { api: PaquetesApi }

export function InicioRoute({ api, session }: Readonly<CatalogoRouteProps & { session: SesionAutenticada | null }>) {
  const navigate = useNavigate()
  const { openPackage, search } = useNavigation()
  const comprador = useCompradorContext()
  return <HomePage api={api} onVerDetalle={openPackage} onSearch={search} favoritos={comprador?.favoritos} compra={comprador?.compra}
    invitacion={session ? undefined : { onRegister: () => navigate(rutas.registro), onLogin: () => navigate(rutas.login) }} />
}

export function ResultadosRoute({ api }: Readonly<CatalogoRouteProps>) {
  const navigate = useNavigate()
  const { busqueda, changeSearch, openPackage } = useNavigation()
  const comprador = useCompradorContext()
  return <ResultadosPage api={api} onVerDetalle={openPackage} filtros={busqueda} onFiltrosChange={changeSearch}
    onBack={() => navigate(rutas.inicio)} favoritos={comprador?.favoritos} compra={comprador?.compra} />
}

/** Only positive safe integers are package IDs; anything else shows not-found without a request. */
function parsePaqueteId(value: string | undefined): number | null {
  if (!value || !/^[1-9]\d*$/.test(value)) return null
  const id = Number(value)
  return Number.isSafeInteger(id) ? id : null
}

export function PaqueteRoute({ api }: Readonly<CatalogoRouteProps>) {
  const paqueteId = parsePaqueteId(useParams().id)
  const { backToList } = useNavigation()
  const comprador = useCompradorContext()
  return <PaquetePage key={paqueteId} api={api} paqueteId={paqueteId} onBack={backToList} favoritos={comprador?.favoritos} compra={comprador?.compra} />
}

/** Rendered behind `RequireRole role="COMPRADOR"`, so the buyer context is present. */
export function FavoritosRoute() {
  const navigate = useNavigate()
  const { openPackage } = useNavigation()
  const comprador = useCompradorContext()
  if (!comprador) return null
  return <FavoritosPage paquetes={comprador.paquetesFavoritos} onVerDetalle={openPackage} favoritos={comprador.favoritos}
    compra={comprador.compra} onHome={() => navigate(rutas.inicio)} />
}

/** Rendered behind `RequireRole role="COMPRADOR"`, so the buyer context is present. */
export function ComprasRoute() {
  const navigate = useNavigate()
  const comprador = useCompradorContext()
  if (!comprador) return null
  const { compras, loading, error, reload } = comprador.historial
  return <ComprasPage compras={compras} loading={loading} error={error} onRetry={reload} onHome={() => navigate(rutas.inicio)} />
}
