import { useCallback, useMemo } from 'react'
import type { ComprasApi } from '../api/compras'
import type { CompraResponseDTO } from '../types/compra'
import { useRemoteData } from './useRemoteData'

/** Purchases of the signed-in buyer, plus how many times each package was bought. */
export function useCompras(api: ComprasApi) {
  const load = useCallback(() => api.listar(), [api])
  const { data, loading, error, reload, updateData } = useRemoteData(load, [])
  const comprados = useMemo(() => {
    const conteo = new Map<number, number>()
    for (const { paquete } of data) conteo.set(paquete.id, (conteo.get(paquete.id) ?? 0) + 1)
    return conteo as ReadonlyMap<number, number>
  }, [data])

  // Repeating an operation returns the original purchase, so the same id must not be counted twice.
  const registrar = useCallback((compra: CompraResponseDTO) => updateData((current) =>
    current.some((item) => item.id === compra.id) ? current : [compra, ...current]), [updateData])

  return { compras: data, comprados, loading, error, reload, registrar }
}
