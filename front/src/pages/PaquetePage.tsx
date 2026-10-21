import { useCallback } from 'react'
import { ApiError } from '../api/http'
import type { PaquetesApi } from '../api/paquetes'
import { PaqueteDetalle } from '../components/paquetes/PaqueteDetalle'
import { useRemoteData } from '../hooks/useRemoteData'
import type { CompraControls } from '../types/compra'
import type { FavoritosControls } from '../types/favorito'

type PaquetePageProps = {
  api: PaquetesApi
  paqueteId: number | null
  onBack: () => void
  favoritos?: FavoritosControls
  compra?: CompraControls
}

export function PaquetePage({ api, paqueteId, onBack, favoritos, compra }: Readonly<PaquetePageProps>) {
  const load = useCallback(async () => {
    if (paqueteId === null) return null
    try {
      return await api.obtener(paqueteId)
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) return null
      throw error
    }
  }, [api, paqueteId])
  const { data: paquete, loading, error, reload } = useRemoteData(load, null)

  return <section className="mx-auto max-w-4xl space-y-6" aria-label="Detalle del paquete">
    <button type="button" onClick={onBack} className="font-semibold text-sky-700 hover:text-sky-900">← Volver al listado</button>
    {loading ? <p role="status" className="py-16 text-center text-slate-500">Cargando paquete...</p>
      : error ? <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center">
        <p role="alert" className="text-red-700">No se pudo cargar el paquete. {error}</p>
        <button type="button" onClick={reload} className="mt-4 rounded-lg bg-sky-700 px-5 py-2.5 font-semibold text-white hover:bg-sky-800">Reintentar</button>
      </div>
      : paquete ? <PaqueteDetalle key={paquete.id} paquete={paquete} favoritos={favoritos} compra={compra} />
        : <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center">
          <h1 className="text-2xl font-bold text-slate-900">Paquete no encontrado</h1>
          <p className="mt-3 text-slate-600">El paquete que buscás no existe o ya no está disponible.</p>
        </div>}
  </section>
}
