import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import { useFloatingPanel } from '../../hooks/useFloatingPanel'

export type SelectOption = {
  value: string
  label: string
  /** Secondary line under the label. */
  detalle?: string
  /** Short status shown at the right, e.g. the seats left. */
  insignia?: { texto: string; tono: 'ok' | 'poco' }
}

const insigniaClass = { ok: 'bg-emerald-50 text-emerald-700', poco: 'bg-amber-50 text-amber-800' }

type SelectProps = {
  label: string
  value: string
  options: readonly SelectOption[]
  onChange: (value: string) => void
  placeholder: string
  /** Shown instead of the options when there are none. */
  emptyMessage?: string
  disabled?: boolean
}

const triggerClass = 'mt-1 flex w-full items-center justify-between gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-left text-slate-900 outline-none focus:border-sky-600 focus:ring-2 focus:ring-sky-100 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400'

/** Accessible single-choice dropdown (select-only combobox) so the options can be styled and described. */
export function Select({ label, value, options, onChange, placeholder, emptyMessage = 'No hay opciones disponibles', disabled = false }: Readonly<SelectProps>) {
  const id = useId()
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(0)
  const root = useRef<HTMLDivElement>(null)
  const trigger = useRef<HTMLButtonElement>(null)
  const panel = useFloatingPanel(trigger, open)
  const selected = options.find((option) => option.value === value)

  useEffect(() => {
    if (!open) return
    const close = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', close)
    return () => document.removeEventListener('pointerdown', close)
  }, [open])

  const show = () => {
    setActive(Math.max(0, options.findIndex((option) => option.value === value)))
    setOpen(true)
  }
  const choose = (option: SelectOption) => {
    onChange(option.value)
    setOpen(false)
  }
  const onKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    const last = options.length - 1
    switch (event.key) {
      case 'ArrowDown': case 'ArrowUp': {
        event.preventDefault()
        if (!open) return show()
        setActive((current) => Math.min(last, Math.max(0, current + (event.key === 'ArrowDown' ? 1 : -1))))
        return
      }
      case 'Home': case 'End':
        if (!open) return
        event.preventDefault()
        return setActive(event.key === 'Home' ? 0 : last)
      case 'Enter': case ' ':
        event.preventDefault()
        if (!open) return show()
        if (options[active]) choose(options[active])
        else setOpen(false)
        return
      case 'Escape':
        // Closing the list must not also close the dialog that contains the form.
        if (open) {
          event.preventDefault()
          event.stopPropagation()
          setOpen(false)
        }
        return
      case 'Tab':
        setOpen(false)
    }
  }

  return <div ref={root} className="relative text-sm font-medium">
    <span id={`${id}-label`}>{label}</span>
    <button ref={trigger} type="button" role="combobox" aria-haspopup="listbox" aria-expanded={open} aria-controls={`${id}-list`}
      aria-labelledby={`${id}-label ${id}-value`} aria-activedescendant={open && options[active] ? `${id}-option-${active}` : undefined}
      className={triggerClass} disabled={disabled} onClick={() => (open ? setOpen(false) : show())} onKeyDown={onKeyDown}>
      <span id={`${id}-value`} className="min-w-0 flex-1">
        <span className={`block truncate ${selected ? '' : 'font-normal text-slate-500'}`}>{selected?.label ?? placeholder}</span>
        {selected?.detalle && <span className="block truncate text-xs font-normal text-slate-500">{selected.detalle}</span>}
      </span>
      <span aria-hidden="true" className="text-xs text-slate-500">{open ? '▲' : '▼'}</span>
    </button>
    {open && <ul id={`${id}-list`} role="listbox" aria-labelledby={`${id}-label`} style={panel}
      className="z-20 overflow-y-auto rounded-xl border border-slate-200 bg-white p-1 shadow-xl">
      {options.length === 0 && <li className="px-3 py-3 text-center font-normal text-slate-500">{emptyMessage}</li>}
      {options.map((option, index) => <li key={option.value} id={`${id}-option-${index}`} role="option" aria-selected={option.value === value}
        onMouseDown={(event) => event.preventDefault()} onMouseEnter={() => setActive(index)} onClick={() => choose(option)} onKeyDown={onKeyDown}
        className={`flex cursor-pointer items-center gap-3 rounded-lg px-3 py-2.5 ${index === active ? 'bg-sky-50' : ''}`}>
        <span className="min-w-0 flex-1">
          <span className={`block text-slate-900 ${option.value === value ? 'font-semibold' : 'font-medium'}`}>{option.label}</span>
          {option.detalle && <span className="block text-xs font-normal text-slate-500">{option.detalle}</span>}
        </span>
        {option.insignia && <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold ${insigniaClass[option.insignia.tono]}`}>{option.insignia.texto}</span>}
        {option.value === value && <span aria-hidden="true" className="text-sky-700">✓</span>}
      </li>)}
    </ul>}
  </div>
}
