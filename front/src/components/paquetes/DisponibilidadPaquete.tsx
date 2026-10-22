import { nivelDisponibilidad, textoDisponibilidad, type NivelDisponibilidad } from './disponibilidad'

// The fewer seats left, the redder the text; kept subtle so the cards stay calm, and every shade keeps 4.5:1 on white.
const CLASES: Record<Exclude<NivelDisponibilidad, 'desconocida'>, string> = {
  alta: 'text-slate-600',
  media: 'text-rose-600',
  baja: 'text-rose-700',
  critica: 'font-semibold text-rose-700',
  agotado: 'font-semibold text-rose-800',
}

type DisponibilidadPaqueteProps = {
  /** null (unknown) renders nothing. */
  disponibilidad: number | null
  /** Lets a buy button reference the text with aria-describedby. */
  id?: string
  className?: string
}

export function DisponibilidadPaquete({ disponibilidad, id, className = '' }: Readonly<DisponibilidadPaqueteProps>) {
  const nivel = nivelDisponibilidad(disponibilidad)
  if (disponibilidad === null || nivel === 'desconocida') return null
  return <p id={id} className={`text-sm ${CLASES[nivel]} ${className}`}>{textoDisponibilidad(disponibilidad)}</p>
}
