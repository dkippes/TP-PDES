import { useCallback, useMemo } from 'react'
import type { ValoracionesApi } from '../api/valoraciones'
import type { ValoracionRequestDTO } from '../types/valoracion'
import { useRemoteData } from './useRemoteData'

export function useValoraciones(api: ValoracionesApi) {
  const load = useCallback(() => api.listarPropias(), [api])
  const { data, loading, error, reload, updateData } = useRemoteData(load, [])
  const porCompra = useMemo(() => new Map(data.map(({ compraId, valoracion }) => [compraId, valoracion])), [data])

  const guardar = async (compraId: number, datos: ValoracionRequestDTO) => {
    const confirmada = await api.guardar(compraId, datos)
    updateData((current) => [confirmada, ...current.filter((item) => item.compraId !== compraId)])
  }
  const eliminar = async (compraId: number) => {
    await api.eliminar(compraId)
    updateData((current) => current.filter((item) => item.compraId !== compraId))
  }

  return { porCompra, loading, error, reload, guardar, eliminar }
}
