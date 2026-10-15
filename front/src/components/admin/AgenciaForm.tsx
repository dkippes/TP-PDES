import { useState, type FormEvent } from 'react'
import type { AgenciasApi } from '../../api/admin'
import type { AgenciaResponseDTO } from '../../types/admin'
import { AdminModal } from './AdminModal'
import { inputClass, primaryClass, secondaryClass } from './controls'

type Props = { api: Pick<AgenciasApi, 'crear' | 'editar'>; agencia: AgenciaResponseDTO | null; onSaved: () => void; onClose: () => void }

export function AgenciaForm({ api, agencia, onSaved, onClose }: Props) {
  const [nombre, setNombre] = useState(agencia?.nombre ?? '')
  const [email, setEmail] = useState(agencia?.email ?? '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const datos = { nombre: nombre.trim(), email: email.trim() }
      if (agencia) await api.editar(agencia.id, datos)
      else await api.crear(datos)
      onSaved()
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No se pudo guardar la agencia.')
    } finally { setBusy(false) }
  }
  return <AdminModal title={agencia ? 'Editar agencia' : 'Nueva agencia'} busy={busy} onClose={onClose}>
    <form onSubmit={submit} className="space-y-4">
      <fieldset disabled={busy} className="space-y-4">
        <label className="block text-sm font-medium">Nombre de la agencia<input className={inputClass} value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={255} /></label>
        <label className="block text-sm font-medium">Correo de la agencia<input className={inputClass} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required maxLength={255} /></label>
      </fieldset>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
        <button type="button" className={secondaryClass} onClick={onClose} disabled={busy}>Cancelar</button>
        <button type="submit" className={primaryClass} disabled={busy}>{busy ? 'Guardando...' : 'Guardar agencia'}</button>
      </div>
    </form>
  </AdminModal>
}
