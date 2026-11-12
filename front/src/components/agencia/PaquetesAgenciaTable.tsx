import type { PaqueteGestionDTO } from '../../types/agencia'
import { Tooltip } from '../Tooltip'
import { formatPrecio, formatRango } from '../paquetes/format'

type Props = {
  paquetes: readonly PaqueteGestionDTO[]
  /** Purchases per package id; a package with purchases is read-only. */
  comprasPorPaquete: ReadonlyMap<number, number>
  onEdit: (paquete: PaqueteGestionDTO) => void
  onDelete: (paquete: PaqueteGestionDTO) => void
}

export function PaquetesAgenciaTable({ paquetes, comprasPorPaquete, onEdit, onDelete }: Readonly<Props>) {
  return <div className="overflow-x-auto">
    <table className="w-full text-left text-sm"><caption className="sr-only">Paquetes de la agencia</caption>
      <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500"><tr>
        <th scope="col" className="px-3 py-3">Viaje</th><th scope="col" className="px-3 py-3">Fechas</th>
        <th scope="col" className="px-3 py-3">Hotel</th><th scope="col" className="px-3 py-3">Vuelos</th>
        <th scope="col" className="px-3 py-3 text-right">Precio</th><th scope="col" className="px-3 py-3 text-right">Lugares</th>
        <th scope="col" className="px-3 py-3"><div className="flex items-center justify-end gap-1.5">
          Acciones
          <Tooltip texto="Los paquetes con compras registradas no se pueden editar ni eliminar: sus acciones aparecen en gris.">{(descripcion) =>
            <button type="button" aria-label="Más información sobre las acciones" {...descripcion}
              className="flex h-4 w-4 items-center justify-center rounded-full border border-slate-400 text-[10px] font-bold normal-case leading-none text-slate-500 hover:border-sky-600 hover:text-sky-700">i</button>}
          </Tooltip>
        </div></th>
      </tr></thead>
      <tbody className="divide-y divide-slate-100">{paquetes.map((paquete) => {
        const compras = comprasPorPaquete.get(paquete.id) ?? 0
        const bloqueado = compras > 0
        return <tr key={paquete.id}>
          <td className="px-3 py-4 font-semibold text-slate-900">{paquete.origen} → {paquete.destino}</td>
          <td className="px-3 py-4 text-slate-500">{formatRango(paquete.fechaInicial, paquete.fechaFinal)}</td>
          <td className="px-3 py-4 text-slate-500">{paquete.hotel.nombre}</td>
          <td className="px-3 py-4 text-slate-500">{paquete.vueloIdaId} / {paquete.vueloVueltaId}</td>
          <td className="px-3 py-4 text-right text-slate-900">{formatPrecio(paquete.precio)}</td>
          <td className="px-3 py-4 text-right text-slate-500">{paquete.disponibilidad ?? 'Sin datos'}</td>
          <td className="px-3 py-4"><div className="flex justify-end gap-3 whitespace-nowrap">
            {accion('Editar', 'text-sky-700', `Editar paquete a ${paquete.destino}`, bloqueado, () => onEdit(paquete))}
            {accion('Eliminar', 'text-red-700', `Eliminar paquete a ${paquete.destino}`, bloqueado, () => onDelete(paquete))}
          </div></td>
        </tr>
      })}</tbody>
    </table>
  </div>
}

/**
 * A package with purchases keeps its action visible but inert: aria-disabled (not disabled) keeps it focusable, and its
 * name says why. The header's info icon explains the rule for the whole column.
 */
function accion(texto: string, color: string, etiqueta: string, bloqueado: boolean, onClick: () => void) {
  if (!bloqueado) {
    return <button type="button" className={`font-medium ${color} hover:underline`} aria-label={etiqueta} onClick={onClick}>{texto}</button>
  }
  return <button type="button" className="cursor-not-allowed font-medium text-slate-400" aria-label={`${etiqueta}, no disponible: ya tiene compras`} aria-disabled="true">{texto}</button>
}
