import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import { useFloatingPanel } from '../../hooks/useFloatingPanel'
import { elegirDia, hoyIso, mesAnterior, mesDe, mesSiguiente, semanasDelMes } from './calendario'
import { formatFechaCompleta } from '../paquetes/format'

type DateRangePickerProps = {
  label: string
  desde: string
  hasta: string
  onChange: (desde: string, hasta: string) => void
  /** ISO dates worth highlighting (days with available flights); the calendar opens at the first one when nothing is chosen. */
  marcados?: ReadonlySet<string>
  /** Days before this ISO date cannot be chosen. */
  minimo?: string
  disabled?: boolean
}

const diasSemana = ['lu', 'ma', 'mi', 'ju', 'vi', 'sá', 'do']
const nombreMes = new Intl.DateTimeFormat('es-AR', { month: 'long', year: 'numeric' })
const triggerClass = 'mt-1 flex w-full items-center justify-between gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-left text-slate-900 outline-none focus:border-sky-600 focus:ring-2 focus:ring-sky-100 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400'
const navClass = 'rounded-md px-3 py-1 text-slate-600 hover:bg-slate-100'

/** Month calendar to pick a "from → to" range of days. */
export function DateRangePicker({ label, desde, hasta, onChange, marcados, minimo = hoyIso(), disabled = false }: Readonly<DateRangePickerProps>) {
  const id = useId()
  const root = useRef<HTMLDivElement>(null)
  const trigger = useRef<HTMLButtonElement>(null)
  const [open, setOpen] = useState(false)
  const [mes, setMes] = useState(() => mesDe(desde || primeroMarcado(marcados, minimo) || minimo))
  const panel = useFloatingPanel(trigger, open, 288)

  useEffect(() => {
    if (!open) return
    const close = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', close)
    return () => document.removeEventListener('pointerdown', close)
  }, [open])

  const toggle = () => {
    if (!open) setMes(mesDe(desde || primeroMarcado(marcados, minimo) || minimo))
    setOpen(!open)
  }
  const elegir = (dia: string) => {
    const rango = elegirDia(desde, hasta, dia)
    onChange(rango.desde, rango.hasta)
    if (rango.hasta !== '') setOpen(false)
  }
  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key !== 'Escape' || !open) return
    // Closing the calendar must not also close the dialog that contains the form.
    event.preventDefault()
    event.stopPropagation()
    setOpen(false)
  }
  const resumen = desde === '' ? 'Elegí las fechas'
    : hasta === '' ? `${formatFechaCompleta(desde)} – elegí el regreso`
      : `${formatFechaCompleta(desde)} – ${formatFechaCompleta(hasta)}`

  return <div ref={root} className="relative text-sm font-medium" onKeyDown={onKeyDown}>
    <span id={`${id}-label`}>{label}</span>
    <button ref={trigger} type="button" aria-haspopup="dialog" aria-expanded={open} aria-labelledby={`${id}-label ${id}-value`} className={triggerClass} disabled={disabled} onClick={toggle}>
      <span id={`${id}-value`} className={desde === '' ? 'font-normal text-slate-500' : ''}>{resumen}</span>
      <span aria-hidden="true">📅</span>
    </button>
    {open && <div role="dialog" aria-label={`Calendario: ${label}`} style={panel} className="z-20 overflow-y-auto rounded-xl border border-slate-200 bg-white p-3 shadow-xl">
      <div className="mb-2 flex items-center justify-between">
        <button type="button" className={navClass} aria-label="Mes anterior" onClick={() => setMes(mesAnterior(mes))}>‹</button>
        <span aria-live="polite" className="font-semibold capitalize text-slate-900">{nombreMes.format(new Date(mes.anio, mes.mes, 1))}</span>
        <button type="button" className={navClass} aria-label="Mes siguiente" onClick={() => setMes(mesSiguiente(mes))}>›</button>
      </div>
      <div className="grid grid-cols-7 text-center text-xs uppercase text-slate-500">{diasSemana.map((dia) => <span key={dia} className="py-1">{dia}</span>)}</div>
      <div className="grid grid-cols-7 gap-y-1">
        {semanasDelMes(mes.anio, mes.mes).flat().map((dia) => {
          const extremo = dia.iso === desde || dia.iso === hasta
          const enRango = desde !== '' && hasta !== '' && dia.iso > desde && dia.iso < hasta
          const bloqueado = dia.iso < minimo
          if (!dia.enMes) return <span key={dia.iso} />
          return <button key={dia.iso} type="button" disabled={bloqueado} aria-pressed={extremo}
            aria-label={`${formatFechaCompleta(dia.iso)}${marcados?.has(dia.iso) ? ', hay vuelos' : ''}`} onClick={() => elegir(dia.iso)}
            className={`relative mx-auto h-9 w-9 rounded-full text-sm font-normal disabled:cursor-not-allowed disabled:text-slate-300 ${extremo ? 'bg-sky-700 font-semibold text-white' : enRango ? 'bg-sky-100 text-sky-900' : 'text-slate-800 hover:bg-slate-100'}`}>
            {dia.dia}
            {marcados?.has(dia.iso) && <span aria-hidden="true" className={`absolute bottom-1 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full ${extremo ? 'bg-white' : 'bg-sky-600'}`} />}
          </button>
        })}
      </div>
      <p className="mt-2 text-xs font-normal text-slate-500">{desde === '' || hasta !== '' ? 'Elegí el día de salida.' : 'Ahora elegí el día de regreso.'} Los puntos marcan días con vuelos.</p>
    </div>}
  </div>
}

function primeroMarcado(marcados: ReadonlySet<string> | undefined, minimo: string): string | undefined {
  return marcados ? [...marcados].filter((dia) => dia >= minimo).sort()[0] : undefined
}
