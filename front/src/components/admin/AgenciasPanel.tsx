import { useCallback, useState } from 'react'
import type { AgenciasApi } from '../../api/admin'
import type { AgenciaResponseDTO } from '../../types/admin'
import { AgenciaForm } from './AgenciaForm'
import { DeleteConfirmation } from './DeleteConfirmation'
import { inputClass, primaryClass, secondaryClass } from './controls'
import { useRemoteData } from '../../hooks/useRemoteData'

export function AgenciasPanel({ api }: { api: AgenciasApi }) {
  const load = useCallback(() => api.listar(), [api])
  const { data, loading, error, reload } = useRemoteData<AgenciaResponseDTO[]>(load, [])
  const [search, setSearch] = useState('')
  const [editor, setEditor] = useState<{ agencia: AgenciaResponseDTO | null } | null>(null)
  const [deleting, setDeleting] = useState<AgenciaResponseDTO | null>(null)
  const [message, setMessage] = useState('')
  const query = search.trim().toLocaleLowerCase()
  const filtered = data.filter((agencia) => `${agencia.nombre} ${agencia.email}`.toLocaleLowerCase().includes(query))

  return <section aria-labelledby="agencias-title" className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
    <div className="flex flex-wrap items-start justify-between gap-4">
      <div><h2 id="agencias-title" className="text-xl font-bold text-slate-900">Agencias</h2><p className="mt-1 text-sm text-slate-500">Gestioná las agencias registradas y sus datos de contacto.</p></div>
      <button type="button" className={primaryClass} onClick={() => { setMessage(''); setEditor({ agencia: null }) }} disabled={loading || !!error}>Nueva agencia</button>
    </div>
    <label className="my-6 block max-w-lg text-sm font-medium">Buscar agencias<input type="search" className={inputClass} placeholder="Nombre o correo" value={search} onChange={(e) => setSearch(e.target.value)} /></label>
    {message && !loading && <p role="status" className="mb-4 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
    {loading ? <p role="status" className="py-10 text-center text-slate-500">Cargando agencias...</p> : error ? <div className="py-6"><p role="alert" className="mb-4 text-sm text-red-700">{error}</p><button type="button" className={secondaryClass} onClick={reload}>Reintentar</button></div> : <>
      <p className="mb-3 text-xs text-slate-500">{filtered.length} de {data.length} agencias</p>
      {filtered.length === 0 ? <p className="rounded-lg bg-slate-50 p-8 text-center text-sm text-slate-500">{data.length === 0 ? 'Todavía no hay agencias registradas.' : 'No hay agencias que coincidan con la búsqueda.'}</p> : <div className="overflow-x-auto">
        <table className="w-full text-left text-sm"><caption className="sr-only">Agencias registradas</caption>
          <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500"><tr><th scope="col" className="px-3 py-3">Agencia</th><th scope="col" className="px-3 py-3">Correo</th><th scope="col" className="px-3 py-3 text-right">Acciones</th></tr></thead>
          <tbody className="divide-y divide-slate-100">{filtered.map((agencia) => <tr key={agencia.id}>
            <td className="px-3 py-4 font-semibold text-slate-900">{agencia.nombre}</td><td className="px-3 py-4 text-slate-500">{agencia.email}</td>
            <td className="px-3 py-4"><div className="flex justify-end gap-3 whitespace-nowrap">
              <button type="button" className="font-medium text-sky-700 hover:underline" aria-label={`Editar agencia ${agencia.nombre}`} onClick={() => { setMessage(''); setEditor({ agencia }) }}>Editar</button>
              <button type="button" className="font-medium text-red-700 hover:underline" aria-label={`Eliminar agencia ${agencia.nombre}`} onClick={() => { setMessage(''); setDeleting(agencia) }}>Eliminar</button>
            </div></td>
          </tr>)}</tbody>
        </table>
      </div>}
    </>}
    {editor && <AgenciaForm api={api} agencia={editor.agencia} onClose={() => setEditor(null)} onSaved={() => {
      setMessage(editor.agencia ? 'Agencia actualizada.' : 'Agencia creada.')
      setEditor(null)
      reload()
    }} />}
    {deleting && <DeleteConfirmation title="Eliminar agencia" description={`¿Querés eliminar ${deleting.nombre}? Esta acción es permanente y también elimina sus paquetes. Si tiene referencias que impiden la baja, la agencia se conservará.`} onClose={() => setDeleting(null)} onDelete={async () => {
      await api.eliminar(deleting.id)
      setDeleting(null)
      setMessage('Agencia eliminada.')
      reload()
    }} />}
  </section>
}
