import { primaryClass, secondaryClass } from './controls'

type AdminFormActionsProps = { busy: boolean; disabled?: boolean; onCancel: () => void; submitLabel: string }

export function AdminFormActions({ busy, disabled = false, onCancel, submitLabel }: AdminFormActionsProps) {
  return <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
    <button type="button" className={secondaryClass} onClick={onCancel} disabled={busy}>Cancelar</button>
    <button type="submit" className={primaryClass} disabled={busy || disabled}>{busy ? 'Guardando...' : submitLabel}</button>
  </div>
}
