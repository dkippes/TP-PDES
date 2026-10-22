import placeholder from '../../assets/paquete-placeholder.svg'
import type { PaqueteResponseDTO } from '../../types/paquete'
import { DisponibilidadPaquete } from './DisponibilidadPaquete'
import { esAgotado } from './disponibilidad'
import { FavoritoButton } from './FavoritoButton'
import { formatPrecio, formatRango } from './format'

type PaqueteCardProps = {
  paquete: PaqueteResponseDTO
  onVerDetalle: (id: number) => void
  favorito?: { guardado: boolean; disabled: boolean; onToggle: () => void }
  onComprar?: () => void
  /** Times the signed-in buyer already bought this package. */
  comprado?: number
  /** Seats left, already resolved against any refresh after a purchase; null when unknown. */
  disponibilidad: number | null
}

export function PaqueteCard({ paquete, onVerDetalle, favorito, onComprar, comprado = 0, disponibilidad }: PaqueteCardProps) {
  const idDisponibilidad = `disponibilidad-paquete-${paquete.id}`

  return <article className="relative flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:border-sky-300 hover:shadow-md">
    <div>
      <img src={placeholder} alt="Ilustración de viaje" className="aspect-[5/3] w-full object-cover" loading="lazy" />
      {comprado > 0 && <span className="absolute left-3 top-3 inline-flex items-center gap-1.5 rounded-full bg-emerald-600 px-3 py-1 text-xs font-semibold text-white shadow">
        <span aria-hidden="true">✓</span>{comprado === 1 ? 'Comprado' : `Comprado ${comprado} veces`}
      </span>}
      {favorito && <FavoritoButton destino={paquete.destino} {...favorito} />}
    </div>
    <div className="flex flex-1 flex-col p-6">
      <h2 className="text-xl font-bold text-slate-900">
        <a href={`/paquetes/${paquete.id}`} aria-label={`Ver detalle del viaje a ${paquete.destino}`} onClick={(event) => {
          if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return
          event.preventDefault()
          onVerDetalle(paquete.id)
        }} className="after:absolute after:inset-0 after:rounded-2xl focus-visible:outline-none focus-visible:after:outline-2 focus-visible:after:outline-offset-[-3px] focus-visible:after:outline-sky-700">{paquete.destino}</a>
        {' '}<span className="text-sm font-normal text-slate-500">{paquete.hotel.pais}</span>
      </h2>
      <p className="mt-1 text-sm font-medium text-sky-700">{formatRango(paquete.fechaInicial, paquete.fechaFinal)}</p>
      <DisponibilidadPaquete disponibilidad={disponibilidad} id={idDisponibilidad} className="mt-1" />
      <p className="mt-3 text-sm leading-6 text-slate-600">
        Viaje desde {paquete.origen} a {paquete.destino}, con vuelos de ida y vuelta y estadía en {paquete.hotel.nombre}.
      </p>
      <div className="mt-auto flex items-end justify-between gap-3 pt-4">
        <p className="text-xs text-slate-500">por {paquete.agencia.nombre}</p>
        <p className="text-2xl font-bold text-slate-900">{formatPrecio(paquete.precio)}</p>
      </div>
      {onComprar && <button type="button" onClick={onComprar} disabled={esAgotado(disponibilidad)}
        aria-label={`Comprar viaje a ${paquete.destino}`} aria-describedby={disponibilidad === null ? undefined : idDisponibilidad}
        className="relative z-10 mt-4 rounded-lg bg-sky-700 px-5 py-2.5 font-semibold text-white enabled:hover:bg-sky-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-700 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500">
        {comprado > 0 ? 'Comprar otra vez' : 'Comprar'}
      </button>}
    </div>
  </article>
}
