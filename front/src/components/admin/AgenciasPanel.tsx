import type { AgenciasApi } from '../../api/admin'
import { useAgenciasPanel } from '../../hooks/admin/useAgenciasPanel'
import { AdminPanel } from './AdminPanel'
import { AdminPanelContent } from './AdminPanelContent'
import { AgenciaForm } from './AgenciaForm'
import { AgenciasTable } from './AgenciasTable'
import { DeleteConfirmation } from './DeleteConfirmation'
import { inputClass } from './controls'

export function AgenciasPanel({ api }: { api: AgenciasApi }) {
  const panel = useAgenciasPanel(api)

  return <AdminPanel titleId="agencias-title" title="Agencias" description="Gestioná las agencias registradas y sus datos de contacto."
    actionLabel="Nueva agencia" onCreate={() => panel.openEditor(null)} createDisabled={panel.loading || !!panel.error}>
    <label className="my-6 block max-w-lg text-sm font-medium">Buscar agencias<input type="search" className={inputClass} placeholder="Nombre o correo" value={panel.search} onChange={(event) => panel.setSearch(event.target.value)} /></label>
    <AdminPanelContent loading={panel.loading} loadingMessage="Cargando agencias..." error={panel.error} message={panel.message} onRetry={panel.reload}>
      <p className="mb-3 text-xs text-slate-500">{panel.filtered.length} de {panel.data.length} agencias</p>
      {panel.filtered.length === 0
        ? <p className="rounded-lg bg-slate-50 p-8 text-center text-sm text-slate-500">{panel.data.length === 0 ? 'Todavía no hay agencias registradas.' : 'No hay agencias que coincidan con la búsqueda.'}</p>
        : <AgenciasTable agencias={panel.filtered} onEdit={panel.openEditor} onDelete={panel.requestDelete} />}
    </AdminPanelContent>
    {panel.editor && <AgenciaForm api={api} agencia={panel.editor.agencia} onClose={panel.closeEditor} onSaved={panel.saved} />}
    {panel.deleting && <DeleteConfirmation title="Eliminar agencia"
      description={`¿Querés eliminar ${panel.deleting.nombre}? Esta acción es permanente y también elimina sus paquetes. Si tiene referencias que impiden la baja, la agencia se conservará.`}
      onClose={panel.closeDelete} onDelete={panel.deleteSelected} />}
  </AdminPanel>
}
