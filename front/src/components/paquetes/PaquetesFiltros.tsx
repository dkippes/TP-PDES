import { useState } from 'react'
import type { AgenciaPublicaDTO, PaqueteBusquedaDTO } from '../../types/paquete'

type PaquetesFiltrosProps = {
  filtros: PaqueteBusquedaDTO
  agencias: AgenciaPublicaDTO[]
  onChange: (cambios: PaqueteBusquedaDTO) => void
}

const inputClass = 'mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-sky-600 focus:outline-none focus:ring-2 focus:ring-sky-200'

const aNumero = (texto: string) => (texto.trim() === '' ? undefined : Number(texto))

export function PaquetesFiltros({ filtros, agencias, onChange }: Readonly<PaquetesFiltrosProps>) {
  const [q, setQ] = useState(filtros.q ?? '')
  const [precioMin, setPrecioMin] = useState(filtros.precioMin?.toString() ?? '')
  const [precioMax, setPrecioMax] = useState(filtros.precioMax?.toString() ?? '')
  const min = aNumero(precioMin)
  const max = aNumero(precioMax)
  const invertido = min !== undefined && max !== undefined && min > max

  const handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!invertido) onChange({ q: q.trim() || undefined, precioMin: min, precioMax: max })
  }
  const limpiar = () => {
    setQ(''); setPrecioMin(''); setPrecioMax('')
    onChange({ q: undefined, agenciaId: undefined, precioMin: undefined, precioMax: undefined })
  }

  return <aside aria-label="Filtros" className="h-fit rounded-2xl border border-slate-200 bg-white p-5">
    <form onSubmit={handleSubmit} className="space-y-4">
      <h2 className="text-lg font-bold text-slate-900">Filtros</h2>
      <label className="block text-sm font-medium text-slate-700"><span>Palabra clave</span>
        <input type="search" className={inputClass} placeholder="Hotel, país, agencia..." value={q} onChange={(event) => setQ(event.target.value)} />
      </label>
      <label className="block text-sm font-medium text-slate-700"><span>Agencia</span>
        <select className={inputClass} value={filtros.agenciaId ?? ''} onChange={(event) => onChange({ agenciaId: aNumero(event.target.value) })}>
          <option value="">Todas</option>
          {agencias.map((agencia) => <option key={agencia.id} value={agencia.id}>{agencia.nombre}</option>)}
        </select>
      </label>
      <div className="grid grid-cols-2 gap-3">
        <label className="block text-sm font-medium text-slate-700"><span>Precio mínimo</span>
          <input type="number" min={0} className={inputClass} value={precioMin} onChange={(event) => setPrecioMin(event.target.value)} />
        </label>
        <label className="block text-sm font-medium text-slate-700"><span>Precio máximo</span>
          <input type="number" min={0} className={inputClass} value={precioMax} onChange={(event) => setPrecioMax(event.target.value)} />
        </label>
      </div>
      {invertido && <p role="alert" className="text-sm text-red-700">El precio mínimo no puede superar al máximo.</p>}
      <button type="submit" disabled={invertido} className="w-full rounded-lg bg-sky-700 px-4 py-2.5 font-semibold text-white hover:bg-sky-800 disabled:opacity-50">Aplicar filtros</button>
      <button type="button" onClick={limpiar} className="w-full rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50">Limpiar filtros</button>
    </form>
  </aside>
}
