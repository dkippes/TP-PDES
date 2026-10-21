import type { CompraControls } from '../types/compra'
import { useCallback } from 'react'
import type { PaquetesApi } from '../api/paquetes'
import { FavoritosStatus } from '../components/paquetes/FavoritosStatus'
import { PaquetesFiltros } from '../components/paquetes/PaquetesFiltros'
import { PaquetesResultados } from '../components/paquetes/PaquetesResultados'
import { PaquetesSearchBar } from '../components/paquetes/PaquetesSearchBar'
import { usePaquetesCatalogo } from '../hooks/usePaquetesCatalogo'
import { useRemoteData } from '../hooks/useRemoteData'
import type { FavoritosControls } from '../types/favorito'
import type { AgenciaPublicaDTO, PaqueteBusquedaDTO } from '../types/paquete'

type ResultadosPageProps = {
  api: PaquetesApi
  filtros: PaqueteBusquedaDTO
  onFiltrosChange: (filtros: PaqueteBusquedaDTO) => void
  onBack: () => void
  favoritos?: FavoritosControls
  compra?: CompraControls
}

export function ResultadosPage({ api, filtros, onFiltrosChange, onBack, favoritos, compra }: Readonly<ResultadosPageProps>) {
  const { sugerencias } = usePaquetesCatalogo(api)
  const loadAgencias = useCallback(() => api.listarAgencias(), [api])
  const { data: agencias } = useRemoteData<AgenciaPublicaDTO[]>(loadAgencias, [])
  const clave = JSON.stringify(filtros)
  // Every filter change goes back to the first page unless it is the pagination itself.
  const cambiar = (cambios: PaqueteBusquedaDTO, conservarPagina = false) =>
    onFiltrosChange({ ...filtros, pagina: conservarPagina ? filtros.pagina : undefined, ...cambios })

  return <div className="space-y-6">
    <div className="flex flex-wrap items-baseline justify-between gap-3">
      <h1 className="text-2xl font-bold tracking-tight text-slate-900">Resultados de búsqueda</h1>
      <button type="button" onClick={onBack} className="text-sm font-semibold text-sky-700 hover:text-sky-900">← Volver al inicio</button>
    </div>
    <PaquetesSearchBar key={clave} compact sugerencias={sugerencias} initial={filtros} onSearch={onFiltrosChange} />
    {favoritos && <FavoritosStatus favoritos={favoritos} />}
    <div className="grid gap-6 lg:grid-cols-[16rem_1fr]">
      <PaquetesFiltros key={`filtros-${clave}`} filtros={filtros} agencias={agencias} onChange={cambiar} />
      <PaquetesResultados key={`resultados-${clave}`} api={api} filtros={filtros} agencias={agencias} onChange={cambiar} onClear={() => onFiltrosChange({})} favoritos={favoritos} compra={compra} />
    </div>
  </div>
}
