import { FavoritosStatus } from '../components/paquetes/FavoritosStatus'
import { PaquetesGrid } from '../components/paquetes/PaquetesGrid'
import type { FavoritosControls } from '../types/favorito'
import type { PaqueteResponseDTO } from '../types/paquete'

type FavoritosPageProps = {
  paquetes: PaqueteResponseDTO[]
  favoritos: FavoritosControls
  onHome: () => void
  onComprar: (paquete: PaqueteResponseDTO) => void
}

export function FavoritosPage({ paquetes, favoritos, onHome, onComprar }: FavoritosPageProps) {
  return <section className="space-y-8">
    <div>
      <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Mis favoritos</h1>
      <p className="mt-3 text-slate-600">Los paquetes que guardaste para tu próximo viaje.</p>
    </div>
    <FavoritosStatus favoritos={favoritos} />
    {!favoritos.loading && !favoritos.error && (paquetes.length > 0
      ? <PaquetesGrid paquetes={paquetes} favoritos={favoritos} onComprar={onComprar} />
      : <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center">
        <p className="text-slate-600">Todavía no guardaste paquetes favoritos.</p>
        <button type="button" onClick={onHome} className="mt-5 rounded-lg bg-sky-700 px-5 py-2.5 font-semibold text-white hover:bg-sky-800">Explorar paquetes</button>
      </div>)}
  </section>
}
