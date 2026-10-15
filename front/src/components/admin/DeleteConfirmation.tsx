import { useState } from 'react'
import { AdminModal } from './AdminModal'
import { dangerClass, secondaryClass } from './controls'

type Props = { title: string; description: string; onDelete: () => Promise<void>; onClose: () => void }

export function DeleteConfirmation({ title, description, onDelete, onClose }: Props) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const confirm = async () => {
    setBusy(true)
    setError('')
    try { await onDelete() } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No se pudo eliminar el registro.')
    } finally { setBusy(false) }
  }
  return <AdminModal title={title} busy={busy} onClose={onClose}>
    <p className="text-sm leading-6 text-slate-600">{description}</p>
    {error && <p role="alert" className="mt-4 text-sm text-red-700">{error}</p>}
    <div className="mt-6 flex justify-end gap-3">
      <button type="button" className={secondaryClass} onClick={onClose} disabled={busy}>Cancelar</button>
      <button type="button" className={dangerClass} onClick={confirm} disabled={busy}>{busy ? 'Eliminando...' : 'Confirmar eliminación'}</button>
    </div>
  </AdminModal>
}
