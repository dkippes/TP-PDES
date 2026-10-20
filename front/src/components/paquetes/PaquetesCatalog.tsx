import type { PaquetesApi } from '../../api/paquetes'
import type { FavoritosControls } from '../../types/favorito'
import { PaquetesList } from './PaquetesList'

export function PaquetesCatalog({ api, favoritos }: { api: PaquetesApi; favoritos?: FavoritosControls }) {
  return <section className="space-y-8">
    <div>
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-700">AterrizAR</p>
      <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Todos los paquetes</h1>
      <p className="mt-3 text-slate-600">Conocé los destinos y elegí tu próximo viaje.</p>
    </div>
    <PaquetesList api={api} favoritos={favoritos} />
  </section>
}
