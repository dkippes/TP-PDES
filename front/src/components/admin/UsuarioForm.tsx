import { useId, useState, type FormEvent } from 'react'
import type { UsuariosApi } from '../../api/admin'
import { esRol, type AgenciaResponseDTO, type Rol, type UsuarioResponseDTO } from '../../types/admin'
import { AdminModal } from './AdminModal'
import { inputClass, primaryClass, roleLabels, secondaryClass } from './controls'

type Props = {
  api: Pick<UsuariosApi, 'crear' | 'editar'>
  usuario: UsuarioResponseDTO | null
  agencias: AgenciaResponseDTO[]
  currentUserId: number
  onSaved: (usuario: UsuarioResponseDTO, passwordChanged: boolean) => void
  onClose: () => void
}

type UsuarioFormState = {
  nombre: string; apellido: string; correo: string; direccion: string
  rol: Rol; agenciaId: string; password: string
}

export function UsuarioForm({ api, usuario, agencias, currentUserId, onSaved, onClose }: Props) {
  const [form, setForm] = useState<UsuarioFormState>({
    nombre: usuario?.nombre ?? '', apellido: usuario?.apellido ?? '', correo: usuario?.correo ?? '',
    direccion: usuario?.direccion ?? '', rol: usuario?.rol ?? 'COMPRADOR',
    agenciaId: usuario?.agenciaId?.toString() ?? '', password: '',
  })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const rolId = useId()
  const agenciaId = useId()
  function change<K extends keyof UsuarioFormState>(field: K, value: UsuarioFormState[K]) {
    setForm((current) => ({ ...current, [field]: value }))
  }
  const isSelf = usuario?.id === currentUserId
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    const datos = {
      nombre: form.nombre.trim(), apellido: form.apellido.trim(), correo: form.correo.trim(),
      direccion: form.direccion.trim(), rol: form.rol,
      agenciaId: form.rol === 'AGENTE' ? Number(form.agenciaId) : null,
    }
    try {
      const saved = usuario
        ? await api.editar(usuario.id, { ...datos, ...(form.password ? { password: form.password } : {}) })
        : await api.crear({ ...datos, password: form.password })
      onSaved(saved, usuario !== null && form.password !== '')
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No se pudo guardar el usuario.')
    } finally { setBusy(false) }
  }

  return <AdminModal title={usuario ? 'Editar usuario' : 'Nuevo usuario'} busy={busy} onClose={onClose}>
    <form onSubmit={submit} className="space-y-4">
      <fieldset disabled={busy} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block text-sm font-medium">Nombre<input className={inputClass} value={form.nombre} onChange={(e) => change('nombre', e.target.value)} required maxLength={255} /></label>
          <label className="block text-sm font-medium">Apellido<input className={inputClass} value={form.apellido} onChange={(e) => change('apellido', e.target.value)} required maxLength={255} /></label>
        </div>
        <label className="block text-sm font-medium">Correo electrónico<input className={inputClass} type="email" value={form.correo} onChange={(e) => change('correo', e.target.value)} required maxLength={255} autoComplete="off" /></label>
        <label className="block text-sm font-medium">Dirección<input className={inputClass} value={form.direccion} onChange={(e) => change('direccion', e.target.value)} required maxLength={255} /></label>
        <div><label htmlFor={rolId} className="block text-sm font-medium">Rol</label><select id={rolId} className={inputClass} value={form.rol} onChange={(e) => { if (esRol(e.target.value)) change('rol', e.target.value) }} disabled={isSelf}>
          {Object.entries(roleLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></div>
        {isSelf && <p className="text-xs text-slate-500">Tu propia cuenta debe conservar el rol de administrador.</p>}
        {form.rol === 'AGENTE' && <>
          <div><label htmlFor={agenciaId} className="block text-sm font-medium">Agencia</label><select id={agenciaId} className={inputClass} value={form.agenciaId} onChange={(e) => change('agenciaId', e.target.value)} required>
            <option value="">Seleccioná una agencia</option>
            {agencias.map((agencia) => <option key={agencia.id} value={agencia.id}>{agencia.nombre}</option>)}
          </select></div>
          {agencias.length === 0 && <p className="text-sm text-amber-800">Creá una agencia antes de registrar un agente.</p>}
        </>}
        <label className="block text-sm font-medium">{usuario ? 'Nueva contraseña (opcional)' : 'Contraseña'}<input className={inputClass} type="password" autoComplete="new-password" value={form.password} onChange={(e) => change('password', e.target.value)} required={!usuario} minLength={8} maxLength={72} /></label>
        {usuario && <p className="text-xs text-slate-500">Dejá la contraseña vacía para conservarla. Si la cambiás, el usuario deberá iniciar sesión nuevamente.</p>}
      </fieldset>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
        <button type="button" className={secondaryClass} onClick={onClose} disabled={busy}>Cancelar</button>
        <button type="submit" className={primaryClass} disabled={busy || (form.rol === 'AGENTE' && agencias.length === 0)}>{busy ? 'Guardando...' : 'Guardar usuario'}</button>
      </div>
    </form>
  </AdminModal>
}
