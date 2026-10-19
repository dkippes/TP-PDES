import type { AgenciaPublicaDTO, PaqueteBusquedaDTO } from '../../types/paquete'
import { formatFecha, formatPrecio } from './format'

type FiltrosActivosProps = {
  filtros: PaqueteBusquedaDTO
  agencias: AgenciaPublicaDTO[]
  onChange: (cambios: PaqueteBusquedaDTO) => void
}

export function FiltrosActivos({ filtros, agencias, onChange }: FiltrosActivosProps) {
  const chips: { etiqueta: string; quitar: PaqueteBusquedaDTO }[] = []
  if (filtros.destino) chips.push({ etiqueta: `Destino: ${filtros.destino}`, quitar: { destino: undefined } })
  if (filtros.origen) chips.push({ etiqueta: `Origen: ${filtros.origen}`, quitar: { origen: undefined } })
  if (filtros.q) chips.push({ etiqueta: `Búsqueda: ${filtros.q}`, quitar: { q: undefined } })
  if (filtros.fechaDesde) chips.push({ etiqueta: `Salida desde ${formatFecha(filtros.fechaDesde)}`, quitar: { fechaDesde: undefined } })
  if (filtros.fechaHasta) chips.push({ etiqueta: `Regreso hasta ${formatFecha(filtros.fechaHasta)}`, quitar: { fechaHasta: undefined } })
  if (filtros.agenciaId !== undefined) {
    const nombre = agencias.find((agencia) => agencia.id === filtros.agenciaId)?.nombre ?? `#${filtros.agenciaId}`
    chips.push({ etiqueta: `Agencia: ${nombre}`, quitar: { agenciaId: undefined } })
  }
  if (filtros.precioMin !== undefined) chips.push({ etiqueta: `Desde ${formatPrecio(filtros.precioMin)}`, quitar: { precioMin: undefined } })
  if (filtros.precioMax !== undefined) chips.push({ etiqueta: `Hasta ${formatPrecio(filtros.precioMax)}`, quitar: { precioMax: undefined } })
  if (chips.length === 0) return null

  return <ul className="flex flex-wrap gap-2" aria-label="Filtros activos">
    {chips.map(({ etiqueta, quitar }) => <li key={etiqueta} className="flex items-center gap-1 rounded-full bg-sky-100 py-1 pl-3 pr-1 text-sm text-sky-900">
      {etiqueta}
      <button type="button" onClick={() => onChange(quitar)} aria-label={`Quitar filtro ${etiqueta}`} className="rounded-full px-2 hover:bg-sky-200">✕</button>
    </li>)}
  </ul>
}
