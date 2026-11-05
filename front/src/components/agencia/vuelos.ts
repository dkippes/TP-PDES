import type { VueloDTO } from '../../types/agencia'
import type { SelectOption } from '../forms/Select'
import { formatFechaCompleta } from '../paquetes/format'

export type RangoFechas = { desde: string; hasta: string }

const conLugares = (vuelo: VueloDTO, actualId: number | null) => vuelo.disponibilidad > 0 || vuelo.id === actualId
const dentro = (vuelo: VueloDTO, { desde, hasta }: RangoFechas) => vuelo.fecha >= desde && vuelo.fecha <= hasta

/** Outbound candidates: flights inside the trip dates with seats (plus the one the package already uses, so editing keeps it). */
export function opcionesIda(vuelos: readonly VueloDTO[], rango: RangoFechas, actualId: number | null): VueloDTO[] {
  return vuelos.filter((vuelo) => dentro(vuelo, rango) && conLugares(vuelo, actualId))
}

/** Return candidates: the reverse route inside the trip dates, on or after the outbound date. */
export function opcionesVuelta(vuelos: readonly VueloDTO[], ida: VueloDTO | undefined, rango: RangoFechas, actualId: number | null): VueloDTO[] {
  if (!ida) return []
  return vuelos.filter((vuelo) => dentro(vuelo, rango) && conLugares(vuelo, actualId)
    && vuelo.origen === ida.destino && vuelo.destino === ida.origen && vuelo.fecha >= ida.fecha)
}

/** How a flight reads in a dropdown: route first, then when and with whom, and the seats left. */
export function opcionVuelo(vuelo: VueloDTO): SelectOption {
  const hora = vuelo.hora ? vuelo.hora.slice(0, 5) : ''
  return {
    value: String(vuelo.id),
    label: `${vuelo.origen} → ${vuelo.destino}`,
    detalle: [formatFechaCompleta(vuelo.fecha), hora, vuelo.aerolinea].filter(Boolean).join(' · '),
    insignia: { texto: `${vuelo.disponibilidad} ${vuelo.disponibilidad === 1 ? 'lugar' : 'lugares'}`, tono: vuelo.disponibilidad <= 3 ? 'poco' : 'ok' },
  }
}
