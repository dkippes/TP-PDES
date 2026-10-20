import { useCallback, useMemo } from 'react'
import type { PaquetesApi } from '../api/paquetes'
import type { PaqueteResponseDTO } from '../types/paquete'
import { useRemoteData } from './useRemoteData'

export type SugerenciasBusqueda = { origenes: string[]; destinos: string[] }

const unicos = (valores: string[]) => [...new Set(valores)].sort((a, b) => a.localeCompare(b, 'es'))

/** Loads the whole catalog once and derives the known origins/destinations used as search suggestions. */
export function usePaquetesCatalogo(api: PaquetesApi) {
  const load = useCallback(() => api.listar(), [api])
  const remote = useRemoteData<PaqueteResponseDTO[]>(load, [])
  const sugerencias = useMemo<SugerenciasBusqueda>(() => ({
    origenes: unicos(remote.data.map((paquete) => paquete.origen)),
    destinos: unicos(remote.data.map((paquete) => paquete.destino)),
  }), [remote.data])
  return { ...remote, sugerencias }
}
