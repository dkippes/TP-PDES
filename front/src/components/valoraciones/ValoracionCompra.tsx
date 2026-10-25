import { useState } from 'react'
import type { ValoracionPublicaDTO, ValoracionRequestDTO } from '../../types/valoracion'
import { ValoracionForm } from './ValoracionForm'

type Props = {
  compraId: number
  valoracion?: ValoracionPublicaDTO
  disabled: boolean
  onGuardar: (datos: ValoracionRequestDTO) => Promise<void>
  onEliminar: () => Promise<void>
}

export function ValoracionCompra({ compraId, valoracion, disabled, onGuardar, onEliminar }: Readonly<Props>) {
  const [editando, setEditando] = useState(false)
  const [aviso, setAviso] = useState('')
  return <div className="mt-3 space-y-2">
    {valoracion && <div className="rounded-lg bg-sky-50 p-3 text-slate-700">
      <p className="font-semibold">Tu puntaje: {valoracion.puntaje}/10</p>
      {valoracion.comentario && <p className="mt-1 whitespace-pre-wrap break-words">{valoracion.comentario}</p>}
    </div>}
    <button type="button" disabled={disabled} onClick={() => { setAviso(''); setEditando(true) }}
      className="font-semibold text-sky-700 hover:text-sky-900 disabled:opacity-50">
      {valoracion ? 'Editar valoración' : 'Valorar compra'}
    </button>
    <p role="status" className="text-sm text-emerald-700">{aviso}</p>
    {editando && <ValoracionForm compraId={compraId} valoracion={valoracion} onClose={() => setEditando(false)}
      onGuardar={async (datos) => { await onGuardar(datos); setAviso('Valoración guardada.') }}
      onEliminar={async () => { await onEliminar(); setAviso('Valoración eliminada. Tu compra se conserva.') }} />}
  </div>
}
