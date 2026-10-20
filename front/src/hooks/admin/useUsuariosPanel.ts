import { useCallback, useState } from 'react'
import type { AgenciasApi, UsuariosApi } from '../../api/admin'
import type { AgenciaResponseDTO, UsuarioResponseDTO } from '../../types/admin'
import { useRemoteData } from '../useRemoteData'

export type UsuariosPanelOptions = {
  api: UsuariosApi
  agenciasApi: Pick<AgenciasApi, 'listar'>
  currentUserId: number
  onCurrentUserUpdated: (usuario: UsuarioResponseDTO, passwordChanged: boolean) => void
}
const initial: { usuarios: UsuarioResponseDTO[]; agencias: AgenciaResponseDTO[] } = { usuarios: [], agencias: [] }

export function useUsuariosPanel({ api, agenciasApi, currentUserId, onCurrentUserUpdated }: UsuariosPanelOptions) {
  const load = useCallback(async () => {
    const [usuarios, agencias] = await Promise.all([api.listar(), agenciasApi.listar()])
    return { usuarios, agencias }
  }, [api, agenciasApi])
  const remote = useRemoteData(load, initial)
  const [search, setSearch] = useState('')
  const [rol, setRol] = useState('')
  const [editor, setEditor] = useState<{ usuario: UsuarioResponseDTO | null } | null>(null)
  const [deleting, setDeleting] = useState<UsuarioResponseDTO | null>(null)
  const [detail, setDetail] = useState<UsuarioResponseDTO | null>(null)
  const [message, setMessage] = useState('')
  const query = search.trim().toLocaleLowerCase()
  const filtered = remote.data.usuarios.filter((usuario) => (!rol || usuario.rol === rol)
    && `${usuario.nombre} ${usuario.apellido} ${usuario.correo}`.toLocaleLowerCase().includes(query))
  const nombreAgencia = (id: number | null) => remote.data.agencias.find((agencia) => agencia.id === id)?.nombre ?? '—'
  const openEditor = (usuario: UsuarioResponseDTO | null) => {
    setMessage('')
    setEditor({ usuario })
  }
  const requestDelete = (usuario: UsuarioResponseDTO) => {
    setMessage('')
    setDeleting(usuario)
  }
  const saved = (usuario: UsuarioResponseDTO, passwordChanged: boolean) => {
    setMessage(editor?.usuario ? 'Usuario actualizado.' : 'Usuario creado.')
    setEditor(null)
    remote.reload()
    if (usuario.id === currentUserId) onCurrentUserUpdated(usuario, passwordChanged)
  }
  const deleteSelected = async () => {
    if (!deleting) return
    await api.eliminar(deleting.id)
    setDeleting(null)
    setMessage('Usuario eliminado.')
    remote.reload()
  }

  return {
    ...remote, search, setSearch, rol, setRol, filtered, nombreAgencia, editor, deleting, detail, message,
    openEditor, closeEditor: () => setEditor(null), requestDelete, closeDelete: () => setDeleting(null),
    openDetail: setDetail, closeDetail: () => setDetail(null), saved, deleteSelected,
  }
}
