import { useAgenciaForm, type AgenciaFormOptions } from '../../hooks/admin/useAgenciaForm'
import { FormField } from '../forms/FormField'
import { AdminFormActions } from './AdminFormActions'
import { AdminModal } from './AdminModal'
import { inputClass } from './controls'

type Props = AgenciaFormOptions & { onClose: () => void }

export function AgenciaForm({ api, agencia, onSaved, onClose }: Props) {
  const { nombre, setNombre, email, setEmail, busy, error, submit } = useAgenciaForm({ api, agencia, onSaved })

  return <AdminModal title={agencia ? 'Editar agencia' : 'Nueva agencia'} busy={busy} onClose={onClose}>
    <form onSubmit={submit} className="space-y-4">
      <fieldset disabled={busy} className="space-y-4">
        <FormField label="Nombre de la agencia" className={inputClass} value={nombre} onChange={(event) => setNombre(event.target.value)} required maxLength={255} />
        <FormField label="Correo de la agencia" className={inputClass} type="email" value={email} onChange={(event) => setEmail(event.target.value)} required maxLength={255} />
      </fieldset>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      <AdminFormActions busy={busy} onCancel={onClose} submitLabel="Guardar agencia" />
    </form>
  </AdminModal>
}
