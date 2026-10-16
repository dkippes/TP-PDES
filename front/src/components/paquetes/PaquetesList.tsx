import { useCallback } from 'react'
import type { PaquetesApi } from '../../api/paquetes'
import { useRemoteData } from '../../hooks/useRemoteData'
import type { PaqueteResponseDTO } from '../../types/paquete'
import { PaqueteCard } from './PaqueteCard'

export function PaquetesList({ api }: { api: PaquetesApi }) {
  const load = useCallback(() => api.listar(), [api])
  const { data, loading, error, reload } = useRemoteData<PaqueteResponseDTO[]>(load, [])

  if (loading) return <p role="status" className="py-16 text-center text-slate-500">Cargando paquetes...</p>
  if (error) return <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center">
    <p role="alert" className="text-sm text-red-700">{error}</p>
    <button type="button" onClick={reload} className="mt-5 rounded-lg bg-sky-700 px-5 py-2.5 font-semibold text-white hover:bg-sky-800">Reintentar</button>
  </div>
  if (data.length === 0) return <p className="rounded-2xl border border-slate-200 bg-white p-12 text-center text-slate-500">Todavía no hay paquetes disponibles.</p>

  return <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
    {data.map((paquete) => <PaqueteCard key={paquete.id} paquete={paquete} />)}
  </div>
}
