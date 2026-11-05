import { useCallback, useMemo } from 'react'
import type { AgenciaApi } from '../../api/agencia'
import { useRemoteData } from '../../hooks/useRemoteData'
import type { VentaDTO } from '../../types/agencia'
import { AdminPanelContent } from '../admin/AdminPanelContent'
import { formatFechaCompleta, formatPrecio } from '../paquetes/format'
import { resumirClientes } from './clientes'

const sinVentas: VentaDTO[] = []
const thClass = 'px-3 py-3'

export function VentasPanel({ api }: Readonly<{ api: AgenciaApi }>) {
  const load = useCallback(() => api.ventas(), [api])
  const { data: ventas, loading, error, reload } = useRemoteData(load, sinVentas)
  const clientes = useMemo(() => resumirClientes(ventas), [ventas])
  const total = ventas.reduce((suma, venta) => suma + venta.precio, 0)

  return <section aria-labelledby="ventas-title" className="space-y-8 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
    <div>
      <h2 id="ventas-title" className="text-xl font-bold text-slate-900">Ventas y clientes</h2>
      <p className="mt-1 text-sm text-slate-500">Lo que se vendió de tu agencia, al precio que pagó cada cliente.</p>
    </div>
    <AdminPanelContent loading={loading} loadingMessage="Cargando ventas..." error={error} message="" onRetry={reload}>
      {ventas.length === 0
        ? <p className="rounded-lg bg-slate-50 p-8 text-center text-sm text-slate-500">Todavía no registraste ventas.</p>
        : <>
          <p className="text-sm text-slate-600">
            {ventas.length} {ventas.length === 1 ? 'venta' : 'ventas'} · {clientes.length} {clientes.length === 1 ? 'cliente' : 'clientes'} · Total {formatPrecio(total)}
          </p>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm"><caption className="mb-2 text-left text-sm font-semibold text-slate-900">Ventas</caption>
              <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500"><tr>
                <th scope="col" className={thClass}>Fecha</th><th scope="col" className={thClass}>Viaje</th>
                <th scope="col" className={thClass}>Cliente</th><th scope="col" className={`${thClass} text-right`}>Precio</th>
              </tr></thead>
              <tbody className="divide-y divide-slate-100">{ventas.map((venta) => <tr key={venta.id}>
                <td className="px-3 py-4 text-slate-500">{formatFechaCompleta(venta.fecha)}</td>
                <td className="px-3 py-4 font-semibold text-slate-900">{venta.origen} → {venta.destino}<span className="block text-xs font-normal text-slate-500">{venta.hotel}</span></td>
                <td className="px-3 py-4 text-slate-700">{venta.cliente.nombre} {venta.cliente.apellido}<span className="block text-xs text-slate-500">{venta.cliente.correo}</span></td>
                <td className="px-3 py-4 text-right text-slate-900">{formatPrecio(venta.precio)}</td>
              </tr>)}</tbody>
            </table>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm"><caption className="mb-2 text-left text-sm font-semibold text-slate-900">Clientes</caption>
              <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500"><tr>
                <th scope="col" className={thClass}>Cliente</th><th scope="col" className={thClass}>Correo</th>
                <th scope="col" className={`${thClass} text-right`}>Compras</th><th scope="col" className={`${thClass} text-right`}>Total</th>
                <th scope="col" className={thClass}>Última compra</th>
              </tr></thead>
              <tbody className="divide-y divide-slate-100">{clientes.map(({ cliente, compras, total: pagado, ultimaCompra }) => <tr key={cliente.id}>
                <td className="px-3 py-4 font-semibold text-slate-900">{cliente.nombre} {cliente.apellido}</td>
                <td className="px-3 py-4 text-slate-500">{cliente.correo}</td>
                <td className="px-3 py-4 text-right text-slate-700">{compras}</td>
                <td className="px-3 py-4 text-right text-slate-900">{formatPrecio(pagado)}</td>
                <td className="px-3 py-4 text-slate-500">{formatFechaCompleta(ultimaCompra)}</td>
              </tr>)}</tbody>
            </table>
          </div>
        </>}
    </AdminPanelContent>
  </section>
}
