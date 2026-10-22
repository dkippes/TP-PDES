import { useCallback, useRef, useState } from 'react'
import type { PaquetesApi } from '../api/paquetes'
import type { PaqueteDetalleDTO } from '../types/paquete'

/** Availability re-read after a purchase, by package id; null is a valid value (flying-service did not answer). */
export function useDisponibilidades(api: Pick<PaquetesApi, 'obtener'>) {
  const [disponibilidades, setDisponibilidades] = useState<ReadonlyMap<number, number | null>>(new Map())
  const secuencias = useRef(new Map<number, number>())

  /** Returns the package just read, or null when the read failed or a newer one for the same package superseded it. */
  const refrescar = useCallback(async (paqueteId: number): Promise<PaqueteDetalleDTO | null> => {
    const secuencia = (secuencias.current.get(paqueteId) ?? 0) + 1
    secuencias.current.set(paqueteId, secuencia)
    try {
      const paquete = await api.obtener(paqueteId)
      // Two purchases in a row start two reads; only the latest one may decide the value.
      if (secuencias.current.get(paqueteId) !== secuencia) return null
      setDisponibilidades((actuales) => new Map(actuales).set(paqueteId, paquete.disponibilidad))
      return paquete
    } catch {
      // The purchase already succeeded: keep the previous value instead of reporting a secondary failure.
      return null
    }
  }, [api])

  /** Views that refetch their packages on mount take over from the refreshed values. */
  const limpiar = useCallback(() => setDisponibilidades((actuales) => actuales.size === 0 ? actuales : new Map()), [])

  return { disponibilidades, refrescar, limpiar }
}
