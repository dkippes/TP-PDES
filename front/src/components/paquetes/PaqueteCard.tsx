import placeholder from '../../assets/paquete-placeholder.svg'
import type { PaqueteResponseDTO } from '../../types/paquete'
import { FavoritoButton } from './FavoritoButton'
import { formatPrecio, formatRango } from './format'

type PaqueteCardProps = {
  paquete: PaqueteResponseDTO
  favorito?: { guardado: boolean; disabled: boolean; onToggle: () => void }
}

export function PaqueteCard({ paquete, favorito }: PaqueteCardProps) {
  return <article className="flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="relative">
      <img src={placeholder} alt="Ilustración de viaje" className="aspect-[5/3] w-full object-cover" loading="lazy" />
      {favorito && <FavoritoButton destino={paquete.destino} {...favorito} />}
    </div>
    <div className="flex flex-1 flex-col p-6">
      <h2 className="text-xl font-bold text-slate-900">{paquete.destino} <span className="text-sm font-normal text-slate-500">{paquete.hotel.pais}</span></h2>
      <p className="mt-1 text-sm font-medium text-sky-700">{formatRango(paquete.fechaInicial, paquete.fechaFinal)}</p>
      <p className="mt-3 text-sm leading-6 text-slate-600">
        Viaje desde {paquete.origen} a {paquete.destino}, con vuelos de ida y vuelta y estadía en {paquete.hotel.nombre}.
      </p>
      <div className="mt-auto flex items-end justify-between gap-3 pt-4">
        <p className="text-xs text-slate-500">por {paquete.agencia.nombre}</p>
        <p className="text-2xl font-bold text-slate-900">{formatPrecio(paquete.precio)}</p>
      </div>
    </div>
  </article>
}
