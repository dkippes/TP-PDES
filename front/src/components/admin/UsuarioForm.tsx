import { useId } from 'react'
import { useUsuarioForm, type UsuarioFormOptions } from '../../hooks/admin/useUsuarioForm'
import { esRol, type AgenciaResponseDTO } from '../../types/admin'
import { FormField } from '../forms/FormField'
import { AdminFormActions } from './AdminFormActions'
import { AdminModal } from './AdminModal'
import { inputClass, roleLabels } from './controls'

type Props = UsuarioFormOptions & { agencias: AgenciaResponseDTO[]; onClose: () => void }

export function UsuarioForm({ api, usuario, agencias, currentUserId, onSaved, onClose }: Props) {
  const { form, change, isSelf, busy, error, submit } = useUsuarioForm({ api, usuario, currentUserId, onSaved })
  const rolId = useId()
  const agenciaId = useId()

  return <AdminModal title={usuario ? 'Editar usuario' : 'Nuevo usuario'} busy={busy} onClose={onClose}>
    <form onSubmit={submit} className="space-y-4">
      <fieldset disabled={busy} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Nombre" className={inputClass} value={form.nombre} onChange={(event) => change('nombre', event.target.value)} required maxLength={255} />
          <FormField label="Apellido" className={inputClass} value={form.apellido} onChange={(event) => change('apellido', event.target.value)} required maxLength={255} />
        </div>
        <FormField label="Correo electrónico" className={inputClass} type="email" value={form.correo} onChange={(event) => change('correo', event.target.value)} required maxLength={255} autoComplete="off" />
        <FormField label="Dirección" className={inputClass} value={form.direccion} onChange={(event) => change('direccion', event.target.value)} required maxLength={255} />
        <div><label htmlFor={rolId} className="block text-sm font-medium">Rol</label><select id={rolId} className={inputClass} value={form.rol} onChange={(event) => { if (esRol(event.target.value)) change('rol', event.target.value) }} disabled={isSelf}>
          {Object.entries(roleLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></div>
        {isSelf && <p className="text-xs text-slate-500">Tu propia cuenta debe conservar el rol de administrador.</p>}
        {form.rol === 'AGENTE' && <>
          <div><label htmlFor={agenciaId} className="block text-sm font-medium">Agencia</label><select id={agenciaId} className={inputClass} value={form.agenciaId} onChange={(event) => change('agenciaId', event.target.value)} required>
            <option value="">Seleccioná una agencia</option>
            {agencias.map((agencia) => <option key={agencia.id} value={agencia.id}>{agencia.nombre}</option>)}
          </select></div>
          {agencias.length === 0 && <p className="text-sm text-amber-800">Creá una agencia antes de registrar un agente.</p>}
        </>}
        <FormField label={usuario ? 'Nueva contraseña (opcional)' : 'Contraseña'} className={inputClass} type="password" autoComplete="new-password" value={form.password} onChange={(event) => change('password', event.target.value)} required={!usuario} minLength={8} maxLength={72} />
        {usuario && <p className="text-xs text-slate-500">Dejá la contraseña vacía para conservarla. Si la cambiás, el usuario deberá iniciar sesión nuevamente.</p>}
      </fieldset>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      <AdminFormActions busy={busy} disabled={form.rol === 'AGENTE' && agencias.length === 0} onCancel={onClose} submitLabel="Guardar usuario" />
    </form>
  </AdminModal>
}
