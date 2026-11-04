import { agruparCompras } from '../components/compras/agruparCompras'
import { CompraGrupoCard } from '../components/compras/CompraGrupoCard'
import type { CompraResponseDTO } from '../types/compra'
import type { ValoracionesApi } from '../api/valoraciones'
import { useValoraciones } from '../hooks/useValoraciones'

type ComprasPageProps = {
  compras: CompraResponseDTO[]
  loading: boolean
  error: string
  onRetry: () => void
  onHome: () => void
  valoracionesApi: ValoracionesApi
}

export function ComprasPage({ compras, loading, error, onRetry, onHome, valoracionesApi }: ComprasPageProps) {
  const valoraciones = useValoraciones(valoracionesApi)
  return <section className="space-y-8">
    <div>
      <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Mis compras</h1>
      <p className="mt-3 text-slate-600">Los viajes que ya compraste, agrupados por paquete, con el detalle de cada compra.</p>
    </div>
    {loading && <p role="status" className="text-sm text-slate-500">Cargando compras...</p>}
    {valoraciones.loading && <p role="status" className="text-sm text-slate-500">Cargando tus valoraciones...</p>}
    {valoraciones.error && <div className="rounded-xl border border-red-200 bg-red-50 p-4">
      <p role="alert" className="text-sm text-red-700">No se pudieron cargar tus valoraciones. {valoraciones.error}</p>
      <button type="button" onClick={valoraciones.reload} className="mt-2 font-semibold text-sky-700">Reintentar valoraciones</button>
    </div>}
    {error && <div className="rounded-xl border border-red-200 bg-red-50 p-4">
      <p role="alert" className="text-sm text-red-700">No se pudieron cargar tus compras. {error}</p>
      <button type="button" onClick={onRetry} className="mt-3 font-semibold text-sky-700 hover:text-sky-900">Reintentar compras</button>
    </div>}
    {!loading && !error && (compras.length > 0
      ? <ul className="space-y-4">{agruparCompras(compras).map((grupo) => <CompraGrupoCard key={grupo.paquete.id} grupo={grupo} valoraciones={valoraciones} />)}</ul>
      : <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center">
        <p className="text-slate-600">Todavía no compraste ningún viaje.</p>
        <button type="button" onClick={onHome} className="mt-5 rounded-lg bg-sky-700 px-5 py-2.5 font-semibold text-white hover:bg-sky-800">Explorar paquetes</button>
      </div>)}
  </section>
}
