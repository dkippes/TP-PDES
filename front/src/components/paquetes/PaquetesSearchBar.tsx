import { useId, useState } from 'react'
import type { SugerenciasBusqueda } from '../../hooks/usePaquetesCatalogo'
import type { PaqueteBusquedaDTO } from '../../types/paquete'
import { normalizarTexto } from './format'
import { SugerenciasInput } from './SugerenciasInput'

type PaquetesSearchBarProps = {
  sugerencias: SugerenciasBusqueda
  initial?: PaqueteBusquedaDTO
  onSearch: (filtros: PaqueteBusquedaDTO) => void
  compact?: boolean
}

const MAX_POPULARES = 5
const cajaClass = 'relative flex items-center gap-3 rounded-xl bg-white px-4 py-2 text-slate-900 focus-within:ring-2 focus-within:ring-orange-400'
const campoClass = 'w-full min-w-0 bg-transparent text-sm font-medium outline-none placeholder:font-normal placeholder:text-slate-400'
const etiquetaClass = 'block text-[11px] font-semibold uppercase tracking-wide text-slate-500'
const iconoProps = { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round', 'aria-hidden': true, className: 'h-5 w-5 shrink-0 text-sky-700' } as const

const hoy = () => {
  const ahora = new Date()
  return `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}-${String(ahora.getDate()).padStart(2, '0')}`
}
const conocido = (texto: string, lista: string[]) => lista.find((item) => normalizarTexto(item) === normalizarTexto(texto))

export function PaquetesSearchBar({ sugerencias, initial = {}, onSearch, compact = false }: Readonly<PaquetesSearchBarProps>) {
  const id = useId()
  const [origen, setOrigen] = useState(initial.origen ?? '')
  const [destino, setDestino] = useState(initial.destino ?? '')
  const [fechaDesde, setFechaDesde] = useState(initial.fechaDesde ?? '')
  const [fechaHasta, setFechaHasta] = useState(initial.fechaHasta ?? '')
  const [error, setError] = useState('')

  const handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault()
    const textoOrigen = origen.trim()
    const textoDestino = destino.trim()
    const destinoConocido = conocido(textoDestino, sugerencias.destinos)
    const origenConocido = conocido(textoOrigen, sugerencias.origenes)
    // A free-text destination falls back to the keyword search (q), which also covers hotel, country and agency.
    const q = destinoConocido || !textoDestino ? undefined : textoDestino
    // Origins stay exact: as q they would also match packages that travel to that place.
    if (textoOrigen && !origenConocido && sugerencias.origenes.length > 0) {
      return setError(`No hay paquetes que salgan de "${textoOrigen}". Elegí un origen de la lista.`)
    }
    setError('')
    onSearch({
      ...initial, pagina: undefined,
      q: q ?? initial.q, destino: destinoConocido, origen: origenConocido ?? (textoOrigen || undefined),
      fechaDesde: fechaDesde || undefined, fechaHasta: fechaHasta || undefined,
    })
  }
  const intercambiar = () => { setOrigen(destino); setDestino(origen) }

  return <section className={compact ? 'rounded-2xl bg-sky-800 p-4' : 'rounded-3xl bg-gradient-to-br from-sky-800 to-sky-600 p-6 shadow-lg sm:p-10'}>
    {!compact && <h1 className="mb-6 text-3xl font-bold tracking-tight text-white sm:text-4xl">Encontrá tu paquete</h1>}
    <form role="search" aria-label="Buscar paquetes" onSubmit={handleSubmit} className="grid gap-3 lg:grid-cols-[1fr_1fr_1.4fr_auto]">
      <div className={cajaClass}>
        <svg {...iconoProps}><circle cx="12" cy="12" r="8" /></svg>
        <div className="min-w-0 flex-1">
          <SugerenciasInput id={`${id}-origen`} label="Origen" placeholder="¿Desde dónde salís?" value={origen} opciones={sugerencias.origenes} onChange={setOrigen}
            inputClass={campoClass} labelClass={etiquetaClass} aria-invalid={error !== ''} aria-describedby={error ? `${id}-error` : undefined} />
        </div>
        <button type="button" onClick={intercambiar} aria-label="Intercambiar origen y destino" className="absolute -bottom-5 right-4 z-10 rounded-full border border-slate-200 bg-white p-1.5 text-sky-700 shadow hover:bg-sky-50 lg:-right-5 lg:bottom-auto lg:top-1/2 lg:-translate-y-1/2">
          <svg {...iconoProps} className="h-4 w-4 rotate-90 lg:rotate-0"><path d="M7 7h13M16 3l4 4-4 4M17 17H4M8 13l-4 4 4 4" /></svg>
        </button>
      </div>
      <div className={cajaClass}>
        <svg {...iconoProps}><path d="M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11z" /><circle cx="12" cy="10" r="2.5" /></svg>
        <div className="min-w-0 flex-1">
          <SugerenciasInput id={`${id}-destino`} label="Destino" placeholder="¿A dónde querés ir?" value={destino} opciones={sugerencias.destinos} onChange={setDestino}
            inputClass={campoClass} labelClass={etiquetaClass} />
        </div>
      </div>
      <div className={`${cajaClass} gap-0 divide-x divide-slate-200`}>
        <div className="min-w-0 flex-1 pr-3">
          <label htmlFor={`${id}-desde`} className={etiquetaClass}>Salida desde</label>
          <input id={`${id}-desde`} type="date" className={campoClass} min={hoy()} value={fechaDesde} onChange={(event) => setFechaDesde(event.target.value)} />
        </div>
        <div className="min-w-0 flex-1 pl-3">
          <label htmlFor={`${id}-hasta`} className={etiquetaClass}>Regreso hasta</label>
          <input id={`${id}-hasta`} type="date" className={campoClass} min={fechaDesde || undefined} value={fechaHasta} onChange={(event) => setFechaHasta(event.target.value)} />
        </div>
      </div>
      <button type="submit" className="flex items-center justify-center gap-2 rounded-xl bg-orange-500 px-8 py-3 font-bold text-white hover:bg-orange-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-white">
        <svg {...iconoProps} className="h-5 w-5"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
        Buscar
      </button>
    </form>
    {error && <p id={`${id}-error`} role="alert" className="mt-3 rounded-lg bg-white/90 px-3 py-2 text-sm text-red-700">{error}</p>}
    {!compact && sugerencias.destinos.length > 0 && <div className="mt-6 flex flex-wrap items-center gap-2">
      <span className="text-sm font-medium text-sky-100">Destinos populares</span>
      {sugerencias.destinos.slice(0, MAX_POPULARES).map((item) => <button key={item} type="button" onClick={() => onSearch({ destino: item })} className="rounded-full bg-white/15 px-3 py-1 text-sm font-medium text-white hover:bg-white/25">{item}</button>)}
    </div>}
  </section>
}
