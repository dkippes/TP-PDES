import { useCallback, useMemo, useState } from 'react'
import type { AgenciaApi } from '../../api/agencia'
import type { PaqueteGestionDTO, VentaDTO } from '../../types/agencia'
import { useRemoteData } from '../useRemoteData'

const sinVentas: VentaDTO[] = []
const sinPaquetes = { contenido: [] as PaqueteGestionDTO[], pagina: 0, tamanio: 0, totalElementos: 0, totalPaginas: 0 }

export function usePaquetesAgenciaPanel(api: AgenciaApi, agenciaId: number) {
  const [pagina, setPagina] = useState(0)
  const load = useCallback(() => api.paquetes.listar(agenciaId, pagina), [api, agenciaId, pagina])
  const remote = useRemoteData(load, sinPaquetes)
  const loadHoteles = useCallback(() => api.hoteles(), [api])
  const hoteles = useRemoteData(loadHoteles, [])
  const loadVuelos = useCallback(() => api.vuelos(), [api])
  const vuelos = useRemoteData(loadVuelos, [])
  // Purchases per package: a package that was bought cannot be edited or deleted. If the sales do not load the backend still refuses.
  const loadVentas = useCallback(() => api.ventas(), [api])
  const ventas = useRemoteData(loadVentas, sinVentas)
  const comprasPorPaquete = useMemo(() => {
    const cantidades = new Map<number, number>()
    for (const venta of ventas.data) cantidades.set(venta.paqueteId, (cantidades.get(venta.paqueteId) ?? 0) + 1)
    return cantidades
  }, [ventas.data])
  const [editor, setEditor] = useState<{ paquete: PaqueteGestionDTO | null } | null>(null)
  const [deleting, setDeleting] = useState<PaqueteGestionDTO | null>(null)
  const [message, setMessage] = useState('')
  const openEditor = (paquete: PaqueteGestionDTO | null) => {
    setMessage('')
    setEditor({ paquete })
  }
  const requestDelete = (paquete: PaqueteGestionDTO) => {
    setMessage('')
    setDeleting(paquete)
  }
  const saved = () => {
    setMessage(editor?.paquete ? 'Paquete actualizado.' : 'Paquete creado.')
    setEditor(null)
    // A new package sorts first, so go back to the first page to show it.
    if (pagina === 0) remote.reload()
    else setPagina(0)
  }
  const deleteSelected = async () => {
    if (!deleting) return
    await api.paquetes.eliminar(deleting.id)
    setDeleting(null)
    setMessage('Paquete eliminado.')
    // Deleting the last item of the last page leaves it empty.
    if (remote.data.contenido.length === 1 && pagina > 0) setPagina(pagina - 1)
    else remote.reload()
  }

  return {
    ...remote, pagina, setPagina, hoteles, vuelos, comprasPorPaquete, editor, deleting, message, openEditor, requestDelete,
    closeEditor: () => setEditor(null), closeDelete: () => setDeleting(null), saved, deleteSelected,
  }
}
