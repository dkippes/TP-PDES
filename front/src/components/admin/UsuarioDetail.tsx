import type { UsuarioResponseDTO } from '../../types/admin'
import { AdminModal } from './AdminModal'
import { roleLabels, secondaryClass } from './controls'

type Props = { usuario: UsuarioResponseDTO; nombreAgencia: string; onClose: () => void }

export function UsuarioDetail({ usuario, nombreAgencia, onClose }: Props) {
  const fields = [
    ['Nombre', `${usuario.nombre} ${usuario.apellido}`],
    ['Correo', usuario.correo],
    ['Dirección', usuario.direccion],
    ['Rol', roleLabels[usuario.rol]],
    ['Agencia', nombreAgencia],
  ]

  return <AdminModal title="Detalle de usuario" busy={false} onClose={onClose}>
    <dl className="space-y-4 text-sm">
      {fields.map(([label, value]) => <div key={label}>
        <dt className="font-medium text-slate-500">{label}</dt>
        <dd className="mt-1 break-words text-slate-900">{value}</dd>
      </div>)}
    </dl>
    <button type="button" className={`${secondaryClass} mt-6`} onClick={onClose}>Cerrar</button>
  </AdminModal>
}
