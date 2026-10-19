import { useCallback, useState } from 'react'
import type { AgenciasApi } from '../../api/admin'
import type { AgenciaResponseDTO } from '../../types/admin'
import { useRemoteData } from '../useRemoteData'

export function useAgenciasPanel(api: AgenciasApi) {
  const load = useCallback(() => api.listar(), [api])
  const remote = useRemoteData<AgenciaResponseDTO[]>(load, [])
  const [search, setSearch] = useState('')
  const [editor, setEditor] = useState<{ agencia: AgenciaResponseDTO | null } | null>(null)
  const [deleting, setDeleting] = useState<AgenciaResponseDTO | null>(null)
  const [message, setMessage] = useState('')
  const query = search.trim().toLocaleLowerCase()
  const filtered = remote.data.filter((agencia) => `${agencia.nombre} ${agencia.email}`.toLocaleLowerCase().includes(query))
  const openEditor = (agencia: AgenciaResponseDTO | null) => {
    setMessage('')
    setEditor({ agencia })
  }
  const requestDelete = (agencia: AgenciaResponseDTO) => {
    setMessage('')
    setDeleting(agencia)
  }
  const saved = () => {
    setMessage(editor?.agencia ? 'Agencia actualizada.' : 'Agencia creada.')
    setEditor(null)
    remote.reload()
  }
  const deleteSelected = async () => {
    if (!deleting) return
    await api.eliminar(deleting.id)
    setDeleting(null)
    setMessage('Agencia eliminada.')
    remote.reload()
  }

  return {
    ...remote, search, setSearch, filtered, editor, deleting, message, openEditor, requestDelete,
    closeEditor: () => setEditor(null), closeDelete: () => setDeleting(null), saved, deleteSelected,
  }
}
