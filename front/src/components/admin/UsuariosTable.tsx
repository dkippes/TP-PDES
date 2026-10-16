import type { UsuarioResponseDTO } from '../../types/admin'
import { roleLabels } from './controls'

type Props = {
  usuarios: readonly UsuarioResponseDTO[]
  currentUserId: number
  nombreAgencia: (id: number | null) => string
  onView: (usuario: UsuarioResponseDTO) => void
  onEdit: (usuario: UsuarioResponseDTO) => void
  onDelete: (usuario: UsuarioResponseDTO) => void
}

export function UsuariosTable({ usuarios, currentUserId, nombreAgencia, onView, onEdit, onDelete }: Props) {
  return <div className="overflow-x-auto">
    <table className="w-full text-left text-sm">
      <caption className="sr-only">Usuarios registrados</caption>
      <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th scope="col" className="px-3 py-3">Usuario</th>
          <th scope="col" className="px-3 py-3">Rol</th>
          <th scope="col" className="px-3 py-3">Agencia</th>
          <th scope="col" className="px-3 py-3 text-right">Acciones</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-slate-100">
        {usuarios.map((usuario) => {
          const nombre = `${usuario.nombre} ${usuario.apellido}`
          const isSelf = usuario.id === currentUserId
          return <tr key={usuario.id}>
            <td className="px-3 py-4">
              <span className="block font-semibold text-slate-900">
                {nombre}
                {isSelf && <span className="ml-2 text-xs font-normal text-sky-700">Tu cuenta</span>}
              </span>
              <span className="text-slate-500">{usuario.correo}</span>
            </td>
            <td className="px-3 py-4">
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium">{roleLabels[usuario.rol]}</span>
            </td>
            <td className="px-3 py-4 text-slate-600">{nombreAgencia(usuario.agenciaId)}</td>
            <td className="px-3 py-4">
              <div className="flex justify-end gap-3 whitespace-nowrap">
                <button type="button" className="font-medium text-slate-600 hover:underline"
                  aria-label={`Ver detalle de ${nombre}`} onClick={() => onView(usuario)}>Ver</button>
                <button type="button" className="font-medium text-sky-700 hover:underline"
                  aria-label={`Editar usuario ${nombre}`} onClick={() => onEdit(usuario)}>Editar</button>
                <button type="button" disabled={isSelf}
                  className="font-medium text-red-700 hover:underline disabled:text-slate-400 disabled:no-underline"
                  title={isSelf ? 'No podés eliminar tu propia cuenta' : undefined}
                  aria-label={`Eliminar usuario ${nombre}`} onClick={() => onDelete(usuario)}>Eliminar</button>
              </div>
            </td>
          </tr>
        })}
      </tbody>
    </table>
  </div>
}
