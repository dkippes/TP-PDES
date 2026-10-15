import type { PaquetesApi } from '../api/paquetes'
import { PaquetesList } from '../components/paquetes/PaquetesList'

export function HomePage({ api }: { api: PaquetesApi }) {
  return (
    <section className="space-y-8">
      <div>
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-700">AterrizAR</p>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Todos los paquetes</h1>
        <p className="mt-3 text-slate-600">Conocé los destinos y elegí tu próximo viaje.</p>
      </div>
      <PaquetesList api={api} />
    </section>
  )
}
