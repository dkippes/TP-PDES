import { formatFechaCompleta, formatPrecio, formatRango } from '../paquetes/format'
import type { CompraGrupo } from './agruparCompras'

export function CompraGrupoCard({ grupo }: Readonly<{ grupo: CompraGrupo }>) {
  const { paquete, compras, total } = grupo
  return <li className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
    <div className="flex flex-wrap items-start justify-between gap-4">
      <div>
        <h2 className="text-xl font-bold text-slate-900">{paquete.destino} <span className="text-sm font-normal text-slate-500">{paquete.hotel.pais}</span></h2>
        <p className="mt-1 text-sm font-medium text-sky-700">{formatRango(paquete.fechaInicial, paquete.fechaFinal)}</p>
        <p className="mt-3 text-sm text-slate-600">Vuelos de ida y vuelta desde {paquete.origen} · {paquete.hotel.nombre} · {paquete.agencia.nombre}</p>
      </div>
      <div className="text-right">
        <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-600 px-3 py-1 text-xs font-semibold text-white">
          <span aria-hidden="true">✓</span>{compras.length === 1 ? 'Comprado' : `Comprado ${compras.length} veces`}
        </span>
        <p className="mt-3 text-xs text-slate-500">{compras.length === 1 ? 'Pagaste' : 'Total pagado'}</p>
        <p className="text-2xl font-bold text-slate-900">{formatPrecio(total)}</p>
      </div>
    </div>
    <details className="group mt-5 border-t border-slate-200 pt-4">
      <summary className="cursor-pointer text-sm font-semibold text-sky-700 hover:text-sky-900">
        Ver detalle de {compras.length === 1 ? 'la compra' : `las ${compras.length} compras`}
      </summary>
      <ul className="mt-3 divide-y divide-slate-100 text-sm">
        {compras.map((compra) => <li key={compra.id} className="flex items-center justify-between gap-4 py-2.5">
          <span className="text-slate-700">Compra n.º {compra.id} · {formatFechaCompleta(compra.fecha)}</span>
          <span className="font-semibold text-slate-900">{formatPrecio(compra.precio)}</span>
        </li>)}
      </ul>
    </details>
  </li>
}
