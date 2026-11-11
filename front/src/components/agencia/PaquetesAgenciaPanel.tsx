import type { AgenciaApi } from '../../api/agencia'
import { usePaquetesAgenciaPanel } from '../../hooks/agencia/usePaquetesAgenciaPanel'
import { AdminPanel } from '../admin/AdminPanel'
import { AdminPanelContent } from '../admin/AdminPanelContent'
import { DeleteConfirmation } from '../admin/DeleteConfirmation'
import { Paginacion } from '../paquetes/Paginacion'
import { PaqueteAgenciaForm } from './PaqueteAgenciaForm'
import { PaquetesAgenciaTable } from './PaquetesAgenciaTable'

type Props = { api: AgenciaApi; agenciaId: number }

export function PaquetesAgenciaPanel({ api, agenciaId }: Readonly<Props>) {
  const panel = usePaquetesAgenciaPanel(api, agenciaId)
  const { contenido, totalElementos, totalPaginas } = panel.data

  return <AdminPanel titleId="paquetes-agencia-title" title="Mis paquetes" description="Publicá, editá y eliminá los paquetes de tu agencia."
    actionLabel="Nuevo paquete" onCreate={() => panel.openEditor(null)} createDisabled={panel.loading || !!panel.error || panel.hoteles.loading || panel.vuelos.loading || !!panel.vuelos.error}>
    <div className="mt-6">
      <AdminPanelContent loading={panel.loading} loadingMessage="Cargando paquetes..." error={panel.error} message={panel.message} onRetry={panel.reload}>
        {contenido.length === 0
          ? <p className="rounded-lg bg-slate-50 p-8 text-center text-sm text-slate-500">Todavía no publicaste ningún paquete.</p>
          : <>
            <p className="mb-3 text-xs text-slate-500">{totalElementos} {totalElementos === 1 ? 'paquete' : 'paquetes'}</p>
            <PaquetesAgenciaTable paquetes={contenido} comprasPorPaquete={panel.comprasPorPaquete} onEdit={panel.openEditor} onDelete={panel.requestDelete} />
            <div className="mt-6"><Paginacion pagina={panel.pagina} totalPaginas={totalPaginas} onChange={panel.setPagina} /></div>
          </>}
      </AdminPanelContent>
    </div>
    {panel.hoteles.error && <p role="alert" className="mt-4 text-sm text-red-700">No se pudieron cargar los hoteles. {panel.hoteles.error}</p>}
    {panel.vuelos.error && <p role="alert" className="mt-4 text-sm text-red-700">No se pudieron cargar los vuelos. {panel.vuelos.error} <button type="button" className="font-semibold text-sky-700 hover:underline" onClick={panel.vuelos.reload}>Reintentar</button></p>}
    {panel.editor && <PaqueteAgenciaForm api={api.paquetes} agenciaId={agenciaId} paquete={panel.editor.paquete} hoteles={panel.hoteles.data} vuelos={panel.vuelos.data}
      onClose={panel.closeEditor} onSaved={panel.saved} />}
    {panel.deleting && <DeleteConfirmation title="Eliminar paquete"
      description={`¿Querés eliminar el paquete a ${panel.deleting.destino}? También se quitará de los favoritos. Si ya tiene compras registradas no se podrá eliminar.`}
      onClose={panel.closeDelete} onDelete={panel.deleteSelected} />}
  </AdminPanel>
}
