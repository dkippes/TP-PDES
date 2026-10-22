import { useCallback, useMemo } from 'react'
import type { FavoritosApi } from '../api/favoritos'
import type { FavoritosControls } from '../types/favorito'
import type { PaqueteResponseDTO } from '../types/paquete'
import { useAsyncAction } from './useAsyncAction'
import { useRemoteData } from './useRemoteData'

export function useFavoritos(api: FavoritosApi) {
  const load = useCallback(() => api.listar(), [api])
  const { data, loading, error, reload, updateData } = useRemoteData(load, [])
  const { busy, error: actionError, execute } = useAsyncAction('No se pudo actualizar el favorito.')
  const ids = useMemo(() => new Set(data.map((paquete) => paquete.id)), [data])

  const toggle = (paquete: PaqueteResponseDTO) => {
    if (loading || busy || error) return
    const guardado = ids.has(paquete.id)
    void execute(async () => {
      if (guardado) await api.quitar(paquete.id)
      else await api.guardar(paquete.id)
      updateData((current) => {
        const otros = current.filter((item) => item.id !== paquete.id)
        return guardado ? otros : [paquete, ...otros]
      })
    })
  }

  /** Replaces a saved package with a fresher read of it (e.g. new availability); ignores packages that are not favorites. */
  const actualizar = (paquete: PaqueteResponseDTO) => {
    if (loading || error || !ids.has(paquete.id)) return
    updateData((current) => current.map((item) => item.id === paquete.id ? paquete : item))
  }

  const controls: FavoritosControls = {
    ids, disabled: loading || busy || !!error, loading, busy, error, actionError, toggle, reload,
  }
  return { paquetes: data, controls, actualizar }
}
