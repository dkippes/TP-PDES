// Highest seat count of each level; above MAX_MEDIA availability is plentiful.
export const MAX_CRITICA = 2
export const MAX_BAJA = 5
export const MAX_MEDIA = 10

export type NivelDisponibilidad = 'desconocida' | 'agotado' | 'critica' | 'baja' | 'media' | 'alta'

/** null means flying-service did not answer, which is different from having no seats left. */
export function nivelDisponibilidad(disponibilidad: number | null): NivelDisponibilidad {
  if (disponibilidad === null) return 'desconocida'
  if (disponibilidad <= 0) return 'agotado'
  if (disponibilidad <= MAX_CRITICA) return 'critica'
  if (disponibilidad <= MAX_BAJA) return 'baja'
  if (disponibilidad <= MAX_MEDIA) return 'media'
  return 'alta'
}

export const esAgotado = (disponibilidad: number | null) => nivelDisponibilidad(disponibilidad) === 'agotado'

export function textoDisponibilidad(disponibilidad: number): string {
  if (disponibilidad <= 0) return 'Agotado'
  return disponibilidad === 1 ? '1 lugar disponible' : `${disponibilidad} lugares disponibles`
}

/** A refresh after a purchase wins over the value the package came with; null is a valid refreshed value. */
export function disponibilidadVigente(
  paquete: { id: number; disponibilidad: number | null },
  actualizadas?: ReadonlyMap<number, number | null>,
): number | null {
  return actualizadas?.has(paquete.id) ? actualizadas.get(paquete.id) ?? null : paquete.disponibilidad
}
