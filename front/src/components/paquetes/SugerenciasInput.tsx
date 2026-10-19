import { useState } from 'react'
import type { ComponentProps, KeyboardEvent } from 'react'
import { normalizarTexto } from './format'

type SugerenciasInputProps = Pick<ComponentProps<'input'>, 'aria-invalid' | 'aria-describedby'> & {
  id: string
  label: string
  placeholder: string
  value: string
  opciones: string[]
  onChange: (value: string) => void
  inputClass: string
  labelClass: string
}

// Custom combobox: a native datalist filters by the current value, so a chosen value opens an empty list.
export function SugerenciasInput({ id, label, placeholder, value, opciones, onChange, inputClass, labelClass, ...aria }: SugerenciasInputProps) {
  const [abierto, setAbierto] = useState(false)
  const [activo, setActivo] = useState(-1)
  // Opening the field shows every option so a chosen value can be changed; typing filters them.
  const [filtrando, setFiltrando] = useState(false)
  const buscado = normalizarTexto(value)
  const visibles = filtrando && buscado ? opciones.filter((opcion) => normalizarTexto(opcion).includes(buscado)) : opciones
  const mostrar = abierto && visibles.length > 0
  const listaId = `${id}-opciones`

  const abrir = () => {
    if (!abierto) setFiltrando(false)
    setAbierto(true)
  }
  const elegir = (opcion: string) => {
    onChange(opcion)
    setAbierto(false)
    setActivo(-1)
  }
  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if ((event.key === 'ArrowDown' || event.key === 'ArrowUp') && visibles.length > 0) {
      event.preventDefault()
      const paso = event.key === 'ArrowDown' ? 1 : -1
      abrir()
      setActivo((actual) => actual < 0
        ? (paso > 0 ? 0 : visibles.length - 1)
        : (actual + paso + visibles.length) % visibles.length)
    } else if (event.key === 'Enter' && mostrar && activo >= 0) {
      event.preventDefault()
      elegir(visibles[activo])
    } else if (event.key === 'Escape') {
      setAbierto(false)
    }
  }

  return <>
    <label htmlFor={id} className={labelClass}>{label}</label>
    <input {...aria} id={id} role="combobox" autoComplete="off" aria-autocomplete="list" aria-expanded={mostrar} aria-controls={listaId}
      aria-activedescendant={mostrar && activo >= 0 ? `${listaId}-${activo}` : undefined}
      className={inputClass} placeholder={placeholder} value={value}
      onChange={(event) => { onChange(event.target.value); setAbierto(true); setFiltrando(true); setActivo(-1) }}
      onFocus={abrir} onClick={abrir} onBlur={() => setAbierto(false)} onKeyDown={handleKeyDown} />
    {mostrar && <ul id={listaId} role="listbox" aria-label={label} className="absolute inset-x-0 top-full z-20 mt-2 max-h-64 overflow-auto rounded-xl border border-slate-200 bg-white py-1 shadow-lg">
      {visibles.map((opcion, index) => <li key={opcion} id={`${listaId}-${index}`} role="option" aria-selected={index === activo}
        onMouseDown={(event) => event.preventDefault()} onClick={() => elegir(opcion)}
        className={`cursor-pointer px-4 py-2 text-sm ${index === activo ? 'bg-sky-50 text-sky-800' : 'text-slate-700 hover:bg-slate-50'}`}>
        {opcion}
      </li>)}
    </ul>}
  </>
}
