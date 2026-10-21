import type { CompraControls } from '../../types/compra'
import { useCallback } from 'react'
import type { PaquetesApi } from '../../api/paquetes'
import { useRemoteData } from '../../hooks/useRemoteData'
import type { FavoritosControls } from '../../types/favorito'
import { ordenesPaquete } from '../../types/paquete'
import type { AgenciaPublicaDTO, OrdenPaquete, PaqueteBusquedaDTO, PaquetesPaginaDTO } from '../../types/paquete'
import { FiltrosActivos } from './FiltrosActivos'
import { Paginacion } from './Paginacion'
import { PaquetesGrid } from './PaquetesGrid'

type PaquetesResultadosProps = {
  api: PaquetesApi
  filtros: PaqueteBusquedaDTO
  agencias: AgenciaPublicaDTO[]
  onChange: (cambios: PaqueteBusquedaDTO, conservarPagina?: boolean) => void
  onClear: () => void
  favoritos?: FavoritosControls
  compra?: CompraControls
}

const etiquetasOrden: Record<OrdenPaquete, string> = {
  FECHA_ASC: 'Fecha más próxima', FECHA_DESC: 'Fecha más lejana',
  PRECIO_ASC: 'Menor precio', PRECIO_DESC: 'Mayor precio', DESTINO_ASC: 'Destino A-Z',
}
const sinResultados: PaquetesPaginaDTO = { contenido: [], pagina: 0, tamanio: 12, totalElementos: 0, totalPaginas: 0 }

function resumen(loading: boolean, error: string, total: number) {
  if (loading) return 'Buscando paquetes...'
  if (error) return ''
  return `${total} ${total === 1 ? 'paquete encontrado' : 'paquetes encontrados'}`
}

// Mount with a key derived from the filters: useRemoteData does not return to loading when `load` changes.
export function PaquetesResultados({ api, filtros, agencias, onChange, onClear, favoritos, compra }: Readonly<PaquetesResultadosProps>) {
  const load = useCallback(() => api.buscar(filtros), [api, filtros])
  const { data, loading, error, reload } = useRemoteData<PaquetesPaginaDTO>(load, sinResultados)

  return <section className="space-y-5" aria-label="Resultados">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <output className="font-semibold text-slate-900">{resumen(loading, error, data.totalElementos)}</output>
      <label className="flex items-center gap-2 text-sm font-medium text-slate-700"><span>Ordenar por</span>
        <select className="rounded-lg border border-slate-300 px-3 py-2 text-sm" value={filtros.orden ?? 'FECHA_ASC'} onChange={(event) => onChange({ orden: event.target.value as OrdenPaquete })}>
          {ordenesPaquete.map((orden) => <option key={orden} value={orden}>{etiquetasOrden[orden]}</option>)}
        </select>
      </label>
    </div>
    <FiltrosActivos filtros={filtros} agencias={agencias} onChange={onChange} />
    {error && <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center">
      <p role="alert" className="text-sm text-red-700">{error}</p>
      <button type="button" onClick={reload} className="mt-5 rounded-lg bg-sky-700 px-5 py-2.5 font-semibold text-white hover:bg-sky-800">Reintentar</button>
    </div>}
    {!loading && !error && data.contenido.length === 0 && <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center">
      <p className="text-slate-500">No encontramos paquetes con esos filtros.</p>
      <button type="button" onClick={onClear} className="mt-5 rounded-lg bg-sky-700 px-5 py-2.5 font-semibold text-white hover:bg-sky-800">Limpiar filtros</button>
    </div>}
    <PaquetesGrid paquetes={data.contenido} favoritos={favoritos} compra={compra} className="grid gap-6 md:grid-cols-2" />
    {!loading && !error && <Paginacion pagina={data.pagina} totalPaginas={data.totalPaginas} onChange={(pagina) => onChange({ pagina }, true)} />}
  </section>
}
