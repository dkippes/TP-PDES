import { useId, useState, type FormEvent } from 'react'
import { useAsyncAction } from '../../hooks/useAsyncAction'
import type { ValoracionPublicaDTO, ValoracionRequestDTO } from '../../types/valoracion'
import { Modal } from '../Modal'

type Props = {
  compraId: number
  valoracion?: ValoracionPublicaDTO
  onGuardar: (datos: ValoracionRequestDTO) => Promise<void>
  onEliminar: () => Promise<void>
  onClose: () => void
}

export function ValoracionForm({ compraId, valoracion, onGuardar, onEliminar, onClose }: Readonly<Props>) {
  const id = useId()
  const [puntaje, setPuntaje] = useState(valoracion ? String(valoracion.puntaje) : '')
  const [comentario, setComentario] = useState(valoracion?.comentario ?? '')
  const { busy, error, execute } = useAsyncAction('No se pudo guardar la valoración.')
  const guardar = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void execute(async () => {
      await onGuardar({ puntaje: Number(puntaje), comentario })
      onClose()
    })
  }

  return <Modal title={`${valoracion ? 'Editar valoración' : 'Valorar compra'} n.º ${compraId}`} busy={busy} onClose={onClose}>
    <form onSubmit={guardar} className="space-y-5">
      <p className="text-sm text-slate-600">Tu valoración será pública, sin mostrar tu nombre. Es opcional y no modifica tu compra.</p>
      <div>
        <label htmlFor={`${id}-puntaje`} className="block text-sm font-semibold text-slate-700">Puntaje (0 a 10)</label>
        <input id={`${id}-puntaje`} type="number" min="0" max="10" step="1" required value={puntaje}
          onChange={(event) => setPuntaje(event.target.value)} disabled={busy}
          className="mt-2 w-full rounded-lg border border-slate-300 px-3 py-2 focus:outline-sky-700" />
      </div>
      <div>
        <label htmlFor={`${id}-comentario`} className="block text-sm font-semibold text-slate-700">Comentario (opcional)</label>
        <textarea id={`${id}-comentario`} rows={4} maxLength={255} value={comentario} disabled={busy}
          onChange={(event) => setComentario(event.target.value)} aria-describedby={`${id}-limite`}
          className="mt-2 w-full rounded-lg border border-slate-300 px-3 py-2 focus:outline-sky-700" />
        <p id={`${id}-limite`} className="mt-1 text-xs text-slate-500">Hasta 255 caracteres.</p>
      </div>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      <p role="status" className="sr-only">{busy ? 'Guardando cambios en la valoración' : ''}</p>
      <div className="flex flex-wrap justify-end gap-3">
        {valoracion && <button type="button" disabled={busy} onClick={() => void execute(async () => { await onEliminar(); onClose() })}
          className="rounded-lg px-3 py-2 font-semibold text-red-700 hover:bg-red-50 disabled:opacity-50">Eliminar valoración</button>}
        <button type="button" disabled={busy} onClick={onClose} className="rounded-lg px-3 py-2 font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-50">Cancelar</button>
        <button type="submit" disabled={busy} className="rounded-lg bg-sky-700 px-4 py-2 font-semibold text-white hover:bg-sky-800 disabled:opacity-50">{busy ? 'Guardando...' : 'Guardar valoración'}</button>
      </div>
    </form>
  </Modal>
}
