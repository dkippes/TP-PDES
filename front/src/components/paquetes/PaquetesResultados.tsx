import { useCallback } from 'react'
import type { PaquetesApi } from '../../api/paquetes'
import { useRemoteData } from '../../hooks/useRemoteData'
import { ordenesPaquete } from '../../types/paquete'
import type { AgenciaPublicaDTO, OrdenPaquete, PaqueteBusquedaDTO, PaquetesPaginaDTO } from '../../types/paquete'
import { FiltrosActivos } from './FiltrosActivos'
import { PaqueteCard } from './PaqueteCard'
import { Paginacion } from './Paginacion'

type PaquetesResultadosProps = {
  api: PaquetesApi
  filtros: PaqueteBusquedaDTO
  agencias: AgenciaPublicaDTO[]
  onChange: (cambios: PaqueteBusquedaDTO, conservarPagina?: boolean) => void
  onClear: () => void
}

const etiquetasOrden: Record<OrdenPaquete, string> = {
  FECHA_ASC: 'Fecha más próxima', FECHA_DESC: 'Fecha más lejana',
  PRECIO_ASC: 'Menor precio', PRECIO_DESC: 'Mayor precio', DESTINO_ASC: 'Destino A-Z',
}
const sinResultados: PaquetesPaginaDTO = { contenido: [], pagina: 0, tamanio: 12, totalElementos: 0, totalPaginas: 0 }

// Mount with a key derived from the filters: useRemoteData does not return to loading when `load` changes.
export function PaquetesResultados({ api, filtros, agencias, onChange, onClear }: PaquetesResultadosProps) {
  const load = useCallback(() => api.buscar(filtros), [api, filtros])
  const { data, loading, error, reload } = useRemoteData<PaquetesPaginaDTO>(load, sinResultados)

  return <section className="space-y-5" aria-label="Resultados">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <p role="status" className="font-semibold text-slate-900">
        {loading ? 'Buscando paquetes...' : error ? '' : `${data.totalElementos} ${data.totalElementos === 1 ? 'paquete encontrado' : 'paquetes encontrados'}`}
      </p>
      <label className="flex items-center gap-2 text-sm font-medium text-slate-700">Ordenar por
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
    <div className="grid gap-6 md:grid-cols-2">
      {data.contenido.map((paquete) => <PaqueteCard key={paquete.id} paquete={paquete} />)}
    </div>
    {!loading && !error && <Paginacion pagina={data.pagina} totalPaginas={data.totalPaginas} onChange={(pagina) => onChange({ pagina }, true)} />}
  </section>
}
