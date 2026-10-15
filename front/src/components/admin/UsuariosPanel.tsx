import { useCallback, useState } from 'react'
import type { AgenciasApi, UsuariosApi } from '../../api/admin'
import type { AgenciaResponseDTO, UsuarioResponseDTO } from '../../types/admin'
import { DeleteConfirmation } from './DeleteConfirmation'
import { UsuarioForm } from './UsuarioForm'
import { UsuarioDetail } from './UsuarioDetail'
import { UsuariosTable } from './UsuariosTable'
import { inputClass, primaryClass, roleLabels, secondaryClass } from './controls'
import { useRemoteData } from '../../hooks/useRemoteData'

type Props = {
  api: UsuariosApi
  agenciasApi: Pick<AgenciasApi, 'listar'>
  currentUserId: number
  onCurrentUserUpdated: (usuario: UsuarioResponseDTO, passwordChanged: boolean) => void
}
const initial: { usuarios: UsuarioResponseDTO[]; agencias: AgenciaResponseDTO[] } = { usuarios: [], agencias: [] }

export function UsuariosPanel({ api, agenciasApi, currentUserId, onCurrentUserUpdated }: Props) {
  const load = useCallback(async () => {
    const [usuarios, agencias] = await Promise.all([api.listar(), agenciasApi.listar()])
    return { usuarios, agencias }
  }, [api, agenciasApi])
  const { data, loading, error, reload } = useRemoteData(load, initial)
  const [search, setSearch] = useState('')
  const [rol, setRol] = useState('')
  const [editor, setEditor] = useState<{ usuario: UsuarioResponseDTO | null } | null>(null)
  const [deleting, setDeleting] = useState<UsuarioResponseDTO | null>(null)
  const [detail, setDetail] = useState<UsuarioResponseDTO | null>(null)
  const [message, setMessage] = useState('')
  const query = search.trim().toLocaleLowerCase()
  const filtered = data.usuarios.filter((usuario) => (!rol || usuario.rol === rol)
    && `${usuario.nombre} ${usuario.apellido} ${usuario.correo}`.toLocaleLowerCase().includes(query))
  const agenciaNombre = (id: number | null) => data.agencias.find((agencia) => agencia.id === id)?.nombre ?? '—'
  const saved = (usuario: UsuarioResponseDTO, passwordChanged: boolean) => {
    setMessage(editor?.usuario ? 'Usuario actualizado.' : 'Usuario creado.')
    setEditor(null)
    reload()
    if (usuario.id === currentUserId) onCurrentUserUpdated(usuario, passwordChanged)
  }

  return <section aria-labelledby="usuarios-title" className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
    <div className="flex flex-wrap items-start justify-between gap-4">
      <div><h2 id="usuarios-title" className="text-xl font-bold text-slate-900">Usuarios</h2><p className="mt-1 text-sm text-slate-500">Administrá las cuentas, sus roles y las agencias asociadas.</p></div>
      <button type="button" className={primaryClass} onClick={() => { setMessage(''); setEditor({ usuario: null }) }} disabled={loading || !!error}>Nuevo usuario</button>
    </div>
    <div className="my-6 grid gap-4 sm:grid-cols-[1fr_14rem]">
      <label className="text-sm font-medium">Buscar usuarios<input type="search" className={inputClass} placeholder="Nombre, apellido o correo" value={search} onChange={(e) => setSearch(e.target.value)} /></label>
      <label className="text-sm font-medium">Filtrar por rol<select className={inputClass} value={rol} onChange={(e) => setRol(e.target.value)}><option value="">Todos los roles</option>{Object.entries(roleLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
    </div>
    {message && !loading && <p role="status" className="mb-4 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
    {loading ? <p role="status" className="py-10 text-center text-slate-500">Cargando usuarios...</p> : error ? <div className="py-6"><p role="alert" className="mb-4 text-sm text-red-700">{error}</p><button type="button" className={secondaryClass} onClick={reload}>Reintentar</button></div> : <>
      <p className="mb-3 text-xs text-slate-500">{filtered.length} de {data.usuarios.length} usuarios</p>
      {filtered.length === 0
        ? <p className="rounded-lg bg-slate-50 p-8 text-center text-sm text-slate-500">{data.usuarios.length === 0 ? 'Todavía no hay usuarios registrados.' : 'No hay usuarios que coincidan con los filtros.'}</p>
        : <UsuariosTable usuarios={filtered} currentUserId={currentUserId} nombreAgencia={agenciaNombre}
          onView={setDetail}
          onEdit={(usuario) => { setMessage(''); setEditor({ usuario }) }}
          onDelete={(usuario) => { setMessage(''); setDeleting(usuario) }} />}
    </>}
    {editor && <UsuarioForm api={api} usuario={editor.usuario} agencias={data.agencias} currentUserId={currentUserId} onSaved={saved} onClose={() => setEditor(null)} />}
    {detail && <UsuarioDetail usuario={detail} nombreAgencia={agenciaNombre(detail.agenciaId)} onClose={() => setDetail(null)} />}
    {deleting && <DeleteConfirmation title="Eliminar usuario" description={`¿Querés eliminar a ${deleting.nombre} ${deleting.apellido}? Esta acción es permanente. Si tiene referencias que impiden la baja, se conservará su cuenta.`} onClose={() => setDeleting(null)} onDelete={async () => {
      await api.eliminar(deleting.id)
      setDeleting(null)
      setMessage('Usuario eliminado.')
      reload()
    }} />}
  </section>
}
