import type { AgenciaResponseDTO } from '../../types/admin'

type AgenciasTableProps = {
  agencias: AgenciaResponseDTO[]
  onEdit: (agencia: AgenciaResponseDTO) => void
  onDelete: (agencia: AgenciaResponseDTO) => void
}

export function AgenciasTable({ agencias, onEdit, onDelete }: AgenciasTableProps) {
  return <div className="overflow-x-auto">
    <table className="w-full text-left text-sm"><caption className="sr-only">Agencias registradas</caption>
      <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500"><tr><th scope="col" className="px-3 py-3">Agencia</th><th scope="col" className="px-3 py-3">Correo</th><th scope="col" className="px-3 py-3 text-right">Acciones</th></tr></thead>
      <tbody className="divide-y divide-slate-100">{agencias.map((agencia) => <tr key={agencia.id}>
        <td className="px-3 py-4 font-semibold text-slate-900">{agencia.nombre}</td><td className="px-3 py-4 text-slate-500">{agencia.email}</td>
        <td className="px-3 py-4"><div className="flex justify-end gap-3 whitespace-nowrap">
          <button type="button" className="font-medium text-sky-700 hover:underline" aria-label={`Editar agencia ${agencia.nombre}`} onClick={() => onEdit(agencia)}>Editar</button>
          <button type="button" className="font-medium text-red-700 hover:underline" aria-label={`Eliminar agencia ${agencia.nombre}`} onClick={() => onDelete(agencia)}>Eliminar</button>
        </div></td>
      </tr>)}</tbody>
    </table>
  </div>
}
