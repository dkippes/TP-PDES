import { useState } from 'react'
import placeholder from '../../assets/paquete-placeholder.svg'
import type { CompraControls } from '../../types/compra'
import type { FavoritosControls } from '../../types/favorito'
import type { PaqueteDetalleDTO } from '../../types/paquete'
import { DisponibilidadPaquete } from './DisponibilidadPaquete'
import { disponibilidadVigente, esAgotado } from './disponibilidad'
import { FavoritoButton } from './FavoritoButton'
import { FavoritosStatus } from './FavoritosStatus'
import { formatFechaCompleta, formatPrecio } from './format'

const ID_DISPONIBILIDAD = 'disponibilidad-paquete'

type PaqueteDetalleProps = {
  paquete: PaqueteDetalleDTO
  favoritos?: FavoritosControls
  compra?: CompraControls
}

export function PaqueteDetalle({ paquete, favoritos, compra }: Readonly<PaqueteDetalleProps>) {
  const [fotoFallida, setFotoFallida] = useState(false)
  const foto = paquete.hotel.foto.trim()
  const tieneFoto = !!foto && !fotoFallida
  const comprado = compra?.comprados.get(paquete.id) ?? 0
  const disponibilidad = disponibilidadVigente(paquete, compra?.disponibilidades)
  const agotado = esAgotado(disponibilidad)

  return <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="relative">
      <img src={tieneFoto ? foto : placeholder} alt={tieneFoto ? `Hotel ${paquete.hotel.nombre}` : 'Ilustración de viaje; foto del hotel no disponible'}
        onError={() => setFotoFallida(true)} className="aspect-[5/3] max-h-96 w-full object-cover sm:aspect-[2/1]" />
      {favoritos && <FavoritoButton destino={paquete.destino} guardado={favoritos.ids.has(paquete.id)}
        disabled={favoritos.disabled} onToggle={() => favoritos.toggle(paquete)} />}
    </div>
    <div className="space-y-6 p-6 sm:p-8">
      <header>
        <p className="text-sm font-semibold text-sky-700">{paquete.hotel.pais}</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Viaje a {paquete.destino}</h1>
      </header>
      <dl className="grid gap-5 text-slate-900 sm:grid-cols-2">
        <div><dt className="text-sm text-slate-500">Origen</dt><dd className="mt-1 font-semibold">{paquete.origen}</dd></div>
        <div><dt className="text-sm text-slate-500">Destino</dt><dd className="mt-1 font-semibold">{paquete.destino}</dd></div>
        <div><dt className="text-sm text-slate-500">Fecha de salida</dt><dd className="mt-1 font-semibold"><time dateTime={paquete.fechaInicial}>{formatFechaCompleta(paquete.fechaInicial)}</time></dd></div>
        <div><dt className="text-sm text-slate-500">Fecha de regreso</dt><dd className="mt-1 font-semibold"><time dateTime={paquete.fechaFinal}>{formatFechaCompleta(paquete.fechaFinal)}</time></dd></div>
        <div><dt className="text-sm text-slate-500">Hotel</dt><dd className="mt-1 font-semibold">{paquete.hotel.nombre}</dd></div>
        <div><dt className="text-sm text-slate-500">Agencia</dt><dd className="mt-1 font-semibold">{paquete.agencia.nombre}</dd></div>
      </dl>
      <div className="flex flex-wrap items-center justify-between gap-4 border-t border-slate-200 pt-6">
        <div><p className="text-sm text-slate-500">Precio del paquete</p><p className="text-3xl font-bold text-slate-900">{formatPrecio(paquete.precio)}</p>
          <DisponibilidadPaquete disponibilidad={disponibilidad} id={ID_DISPONIBILIDAD} className="mt-1" /></div>
        {compra && <div className="space-y-2">
          {comprado > 0 && <p className="text-sm font-semibold text-emerald-700">Comprado {comprado} {comprado === 1 ? 'vez' : 'veces'}</p>}
          <button type="button" onClick={() => compra.comprar(paquete)} disabled={agotado} aria-describedby={disponibilidad === null ? undefined : ID_DISPONIBILIDAD}
            className="rounded-lg bg-sky-700 px-6 py-3 font-semibold text-white enabled:hover:bg-sky-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-700 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500">
            {comprado > 0 ? 'Comprar otra vez' : 'Comprar'}
          </button>
        </div>}
      </div>
      {favoritos && <FavoritosStatus favoritos={favoritos} />}
    </div>
  </article>
}
