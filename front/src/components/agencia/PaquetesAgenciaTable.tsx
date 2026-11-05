import type { PaqueteGestionDTO } from '../../types/agencia'
import { formatPrecio, formatRango } from '../paquetes/format'

type Props = {
  paquetes: readonly PaqueteGestionDTO[]
  onEdit: (paquete: PaqueteGestionDTO) => void
  onDelete: (paquete: PaqueteGestionDTO) => void
}

export function PaquetesAgenciaTable({ paquetes, onEdit, onDelete }: Readonly<Props>) {
  return <div className="overflow-x-auto">
    <table className="w-full text-left text-sm"><caption className="sr-only">Paquetes de la agencia</caption>
      <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500"><tr>
        <th scope="col" className="px-3 py-3">Viaje</th><th scope="col" className="px-3 py-3">Fechas</th>
        <th scope="col" className="px-3 py-3">Hotel</th><th scope="col" className="px-3 py-3">Vuelos</th>
        <th scope="col" className="px-3 py-3 text-right">Precio</th><th scope="col" className="px-3 py-3 text-right">Lugares</th>
        <th scope="col" className="px-3 py-3 text-right">Acciones</th>
      </tr></thead>
      <tbody className="divide-y divide-slate-100">{paquetes.map((paquete) => <tr key={paquete.id}>
        <td className="px-3 py-4 font-semibold text-slate-900">{paquete.origen} → {paquete.destino}</td>
        <td className="px-3 py-4 text-slate-500">{formatRango(paquete.fechaInicial, paquete.fechaFinal)}</td>
        <td className="px-3 py-4 text-slate-500">{paquete.hotel.nombre}</td>
        <td className="px-3 py-4 text-slate-500">{paquete.vueloIdaId} / {paquete.vueloVueltaId}</td>
        <td className="px-3 py-4 text-right text-slate-900">{formatPrecio(paquete.precio)}</td>
        <td className="px-3 py-4 text-right text-slate-500">{paquete.disponibilidad ?? 'Sin datos'}</td>
        <td className="px-3 py-4"><div className="flex justify-end gap-3 whitespace-nowrap">
          <button type="button" className="font-medium text-sky-700 hover:underline" aria-label={`Editar paquete a ${paquete.destino}`} onClick={() => onEdit(paquete)}>Editar</button>
          <button type="button" className="font-medium text-red-700 hover:underline" aria-label={`Eliminar paquete a ${paquete.destino}`} onClick={() => onDelete(paquete)}>Eliminar</button>
        </div></td>
      </tr>)}</tbody>
    </table>
  </div>
}
