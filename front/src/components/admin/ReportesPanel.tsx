import type { ReportesApi } from '../../api/admin'
import { useRemoteData } from '../../hooks/useRemoteData'
import type { ReporteAdminDTO } from '../../types/admin'
import { AdminPanelContent } from './AdminPanelContent'

type FilaRanking = { id: number | string; nombre: string; valor: string }

const entero = new Intl.NumberFormat('es-AR')
const promedio = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 })

function cantidad(total: number, singular: string, plural: string) {
  return `${entero.format(total)} ${total === 1 ? singular : plural}`
}

function Ranking({ titulo, filas }: Readonly<{ titulo: string; filas: FilaRanking[] }>) {
  return <div className="min-w-0 rounded-xl border border-slate-200 p-4 sm:p-5">
    <h3 className="font-semibold text-slate-900">{titulo}</h3>
    {filas.length === 0
      ? <p className="mt-3 text-sm text-slate-600">Todavía no hay datos para este ranking.</p>
      : <ol className="mt-2 list-decimal divide-y divide-slate-100 pl-6 text-sm marker:font-semibold marker:text-slate-500">
        {filas.map(({ id, nombre, valor }) => <li key={id} className="py-3 pl-1 last:pb-0">
          <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
            <span className="min-w-0 wrap-break-word font-medium text-slate-900">{nombre}</span>
            <span className="font-semibold tabular-nums text-slate-700">{valor}</span>
          </div>
        </li>)}
      </ol>}
  </div>
}

export function ReportesPanel({ api }: Readonly<{ api: ReportesApi }>) {
  const { data, loading, error, reload } = useRemoteData<ReporteAdminDTO | null>(api.obtener, null)

  return <section aria-labelledby="reportes-title" className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
    <h2 id="reportes-title" className="text-xl font-bold text-slate-900">Reportes</h2>
    <p className="mt-1 text-sm text-slate-500">Top 5 de compras, ventas y valoraciones.</p>
    <AdminPanelContent loading={loading} loadingMessage="Cargando reportes..." error={error} message="" onRetry={reload}>
      {data && <div className="mt-6 grid gap-4 md:grid-cols-2">
        <Ranking titulo="Usuarios con más compras" filas={data.usuariosConMasCompras.map((usuario) => ({
          id: usuario.usuarioId,
          nombre: `${usuario.nombre} ${usuario.apellido}`,
          valor: cantidad(usuario.totalCompras, 'compra', 'compras'),
        }))} />
        <Ranking titulo="Destinos mejor puntuados" filas={data.destinosMejorRankeados.map((destino) => ({
          id: destino.destino,
          nombre: destino.destino,
          valor: `${promedio.format(destino.promedio)}/10 · ${cantidad(destino.totalValoraciones, 'valoración', 'valoraciones')}`,
        }))} />
        <Ranking titulo="Agencias con más ventas" filas={data.agenciasConMasVentas.map((agencia) => ({
          id: agencia.agenciaId,
          nombre: agencia.nombre,
          valor: cantidad(agencia.totalVentas, 'venta', 'ventas'),
        }))} />
        <Ranking titulo="Destinos más vendidos" filas={data.destinosMasVendidos.map((destino) => ({
          id: destino.destino,
          nombre: destino.destino,
          valor: cantidad(destino.totalVentas, 'venta', 'ventas'),
        }))} />
      </div>}
    </AdminPanelContent>
  </section>
}
