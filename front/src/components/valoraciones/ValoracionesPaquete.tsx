import { useCallback } from 'react'
import { requestJson } from '../../api/http'
import { createValoracionesPublicasApi } from '../../api/valoraciones'
import { useRemoteData } from '../../hooks/useRemoteData'

const api = createValoracionesPublicasApi(requestJson)
const fecha = new Intl.DateTimeFormat('es-AR', { dateStyle: 'long' })
const promedio = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 })

export function ValoracionesPaquete({ paqueteId }: Readonly<{ paqueteId: number }>) {
  const load = useCallback(() => api.listar(paqueteId), [paqueteId])
  const { data, loading, error, reload } = useRemoteData(load, null)

  return <section aria-label="Valoraciones del paquete" className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8">
    <h2 className="text-2xl font-bold text-slate-900">Valoraciones del paquete</h2>
    {loading ? <p role="status" className="mt-4 text-slate-500">Cargando valoraciones...</p>
      : error ? <div className="mt-4">
        <p role="alert" className="text-red-700">No se pudieron cargar las valoraciones. {error}</p>
        <button type="button" onClick={reload} className="mt-2 font-semibold text-sky-700">Reintentar valoraciones</button>
      </div>
        : data && (data.total === 0 ? <p className="mt-4 text-slate-600">Este paquete todavía no tiene valoraciones.</p>
          : <>
            <p className="mt-4 text-slate-700"><strong className="text-2xl text-sky-800">{promedio.format(data.promedio!)}/10</strong> · {data.total} {data.total === 1 ? 'valoración' : 'valoraciones'}</p>
            <ul className="mt-5 divide-y divide-slate-200">
              {data.valoraciones.map((valoracion) => <li key={valoracion.id} className="py-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-semibold text-slate-900">{valoracion.puntaje}/10</span>
                  <time dateTime={valoracion.fecha} className="text-sm text-slate-500">{fecha.format(new Date(valoracion.fecha))}</time>
                </div>
                {valoracion.comentario && <p className="mt-2 whitespace-pre-wrap break-words text-slate-700">{valoracion.comentario}</p>}
              </li>)}
            </ul>
          </>)}
  </section>
}
