import placeholder from '../../assets/paquete-placeholder.svg'
import type { PaqueteResponseDTO } from '../../types/paquete'
import { FavoritoButton } from './FavoritoButton'

type PaqueteCardProps = {
  paquete: PaqueteResponseDTO
  favorito?: { guardado: boolean; disabled: boolean; onToggle: () => void }
}

export function PaqueteCard({ paquete, favorito }: PaqueteCardProps) {
  return <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="relative">
      <img src={placeholder} alt="Ilustración de viaje" className="aspect-[5/3] w-full object-cover" loading="lazy" />
      {favorito && <FavoritoButton destino={paquete.destino} {...favorito} />}
    </div>
    <div className="p-6">
      <h2 className="text-xl font-bold text-slate-900">{paquete.destino}</h2>
      <p className="mt-3 text-sm leading-6 text-slate-600">
        Viaje desde {paquete.origen} a {paquete.destino}, con vuelos de ida y vuelta y estadía en {paquete.hotel.nombre}.
      </p>
    </div>
  </article>
}
