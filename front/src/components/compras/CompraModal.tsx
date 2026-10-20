import type { CompraResponseDTO } from '../../types/compra'
import type { PaqueteResponseDTO } from '../../types/paquete'
import { Modal } from '../Modal'
import { formatPrecio, formatRango } from '../paquetes/format'

type CompraModalProps = {
  paquete: PaqueteResponseDTO
  compra: CompraResponseDTO | null
  busy: boolean
  error: string
  onConfirm: () => void
  onClose: () => void
}

function etiquetaConfirmar(busy: boolean, error: string) {
  if (busy) return 'Comprando...'
  return error ? 'Reintentar' : 'Confirmar compra'
}

export function CompraModal({ paquete, compra, busy, error, onConfirm, onClose }: Readonly<CompraModalProps>) {
  if (compra) return <Modal title="Compra confirmada" busy={false} onClose={onClose}>
    <p role="status" className="text-slate-700">
      Compraste el viaje a <strong>{compra.paquete.destino}</strong> por <strong>{formatPrecio(compra.precio)}</strong>.
    </p>
    <p className="mt-2 text-sm text-slate-500">Número de compra: {compra.id}</p>
    <div className="mt-6 flex justify-end">
      <button type="button" onClick={onClose} className="rounded-lg bg-sky-700 px-5 py-2.5 font-semibold text-white hover:bg-sky-800">Cerrar</button>
    </div>
  </Modal>

  return <Modal title="Confirmar compra" busy={busy} onClose={onClose}>
    <dl className="space-y-2 text-sm text-slate-700">
      <div><dt className="inline font-semibold">Destino: </dt><dd className="inline">{paquete.destino}, {paquete.hotel.pais}</dd></div>
      <div><dt className="inline font-semibold">Fechas: </dt><dd className="inline">{formatRango(paquete.fechaInicial, paquete.fechaFinal)}</dd></div>
      <div><dt className="inline font-semibold">Vuelos: </dt><dd className="inline">ida y vuelta desde {paquete.origen}</dd></div>
      <div><dt className="inline font-semibold">Hotel: </dt><dd className="inline">{paquete.hotel.nombre}</dd></div>
      <div><dt className="inline font-semibold">Agencia: </dt><dd className="inline">{paquete.agencia.nombre}</dd></div>
    </dl>
    <p className="mt-5 flex items-baseline justify-between border-t border-slate-200 pt-4">
      <span className="text-sm text-slate-600">Total</span>
      <span className="text-2xl font-bold text-slate-900">{formatPrecio(paquete.precio)}</span>
    </p>
    {error && <p role="alert" className="mt-4 text-sm text-red-700">{error}</p>}
    <p aria-live="polite" className="sr-only">{busy ? 'Procesando la compra' : ''}</p>
    <div className="mt-6 flex justify-end gap-3">
      <button type="button" onClick={onClose} disabled={busy}
        className="rounded-lg px-4 py-2.5 font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-50">Cancelar</button>
      <button type="button" onClick={onConfirm} disabled={busy}
        className="rounded-lg bg-sky-700 px-5 py-2.5 font-semibold text-white hover:bg-sky-800 disabled:cursor-wait disabled:opacity-60">
        {etiquetaConfirmar(busy, error)}
      </button>
    </div>
  </Modal>
}
