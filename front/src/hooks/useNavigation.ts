import { useLocation, useNavigate } from 'react-router'
import { rutas } from '../routes/rutas'
import type { PaqueteBusquedaDTO } from '../types/paquete'

type NavigationState = { busqueda?: PaqueteBusquedaDTO; desdeListado?: boolean }

// Stable identity: the results reload whenever the filters object changes.
const sinFiltros: PaqueteBusquedaDTO = {}

/** Search filters and the source list live in history state, so back/forward and reloads keep them. */
export function useNavigation() {
  const navigate = useNavigate()
  const state = (useLocation().state ?? {}) as NavigationState

  const search = (busqueda: PaqueteBusquedaDTO) => navigate(rutas.resultados, { state: { busqueda } })
  const changeSearch = (busqueda: PaqueteBusquedaDTO) => navigate(rutas.resultados, { replace: true, state: { busqueda } })
  const openPackage = (paqueteId: number) => navigate(rutas.paquete(paqueteId), { state: { desdeListado: true } })
  const backToList = () => {
    if (state.desdeListado) void navigate(-1)
    else void navigate(rutas.inicio)
  }

  return { busqueda: state.busqueda ?? sinFiltros, search, changeSearch, openPackage, backToList }
}
