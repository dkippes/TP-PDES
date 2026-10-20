import type { PaqueteResponseDTO } from '../../types/paquete'
import type { FavoritosControls } from '../../types/favorito'
import { PaquetesGrid } from './PaquetesGrid'

type PaquetesListProps = {
  paquetes: PaqueteResponseDTO[]
  loading: boolean
  error: string
  onRetry: () => void
  favoritos?: FavoritosControls
  onComprar?: (paquete: PaqueteResponseDTO) => void
}

export function PaquetesList({ paquetes, loading, error, onRetry, favoritos, onComprar }: Readonly<PaquetesListProps>) {
  if (loading) return <p role="status" className="py-16 text-center text-slate-500">Cargando paquetes...</p>
  if (error) return <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center">
    <p role="alert" className="text-sm text-red-700">{error}</p>
    <button type="button" onClick={onRetry} className="mt-5 rounded-lg bg-sky-700 px-5 py-2.5 font-semibold text-white hover:bg-sky-800">Reintentar</button>
  </div>
  if (paquetes.length === 0) return <p className="rounded-2xl border border-slate-200 bg-white p-12 text-center text-slate-500">Todavía no hay paquetes disponibles.</p>

  return <PaquetesGrid paquetes={paquetes} favoritos={favoritos} onComprar={onComprar} />
}
