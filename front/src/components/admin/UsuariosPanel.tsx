import { useUsuariosPanel, type UsuariosPanelOptions } from '../../hooks/admin/useUsuariosPanel'
import { AdminPanel } from './AdminPanel'
import { AdminPanelContent } from './AdminPanelContent'
import { DeleteConfirmation } from './DeleteConfirmation'
import { UsuarioForm } from './UsuarioForm'
import { UsuarioDetail } from './UsuarioDetail'
import { UsuariosFilters } from './UsuariosFilters'
import { UsuariosTable } from './UsuariosTable'

export function UsuariosPanel(props: UsuariosPanelOptions) {
  const panel = useUsuariosPanel(props)

  return <AdminPanel titleId="usuarios-title" title="Usuarios" description="Administrá las cuentas, sus roles y las agencias asociadas."
    actionLabel="Nuevo usuario" onCreate={() => panel.openEditor(null)} createDisabled={panel.loading || !!panel.error}>
    <UsuariosFilters search={panel.search} onSearchChange={panel.setSearch} rol={panel.rol} onRolChange={panel.setRol} />
    <AdminPanelContent loading={panel.loading} loadingMessage="Cargando usuarios..." error={panel.error} message={panel.message} onRetry={panel.reload}>
      <p className="mb-3 text-xs text-slate-500">{panel.filtered.length} de {panel.data.usuarios.length} usuarios</p>
      {panel.filtered.length === 0
        ? <p className="rounded-lg bg-slate-50 p-8 text-center text-sm text-slate-500">{panel.data.usuarios.length === 0 ? 'Todavía no hay usuarios registrados.' : 'No hay usuarios que coincidan con los filtros.'}</p>
        : <UsuariosTable usuarios={panel.filtered} currentUserId={props.currentUserId} nombreAgencia={panel.nombreAgencia}
          onView={panel.openDetail} onEdit={panel.openEditor} onDelete={panel.requestDelete} />}
    </AdminPanelContent>
    {panel.editor && <UsuarioForm api={props.api} usuario={panel.editor.usuario} agencias={panel.data.agencias}
      currentUserId={props.currentUserId} onSaved={panel.saved} onClose={panel.closeEditor} />}
    {panel.detail && <UsuarioDetail usuario={panel.detail} nombreAgencia={panel.nombreAgencia(panel.detail.agenciaId)} onClose={panel.closeDetail} />}
    {panel.deleting && <DeleteConfirmation title="Eliminar usuario"
      description={`¿Querés eliminar a ${panel.deleting.nombre} ${panel.deleting.apellido}? Esta acción es permanente. Si tiene referencias que impiden la baja, se conservará su cuenta.`}
      onClose={panel.closeDelete} onDelete={panel.deleteSelected} />}
  </AdminPanel>
}
