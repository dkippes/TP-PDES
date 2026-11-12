import { useId, useRef, useState, type ReactNode } from 'react'
import { useFloatingPanel } from '../hooks/useFloatingPanel'

type TooltipProps = {
  texto: string
  /** Receives the props that link the trigger to the tooltip text. */
  children: (descripcion: { 'aria-describedby': string }) => ReactNode
}

/** Hint shown on hover or keyboard focus. It floats with fixed position so scrolling tables do not clip it. */
export function Tooltip({ texto, children }: Readonly<TooltipProps>) {
  const id = useId()
  const anchor = useRef<HTMLSpanElement>(null)
  const [hover, setHover] = useState(false)
  const [focus, setFocus] = useState(false)
  const visible = hover || focus
  const style = useFloatingPanel(anchor, visible, 224)

  return <span ref={anchor} role="presentation" className="inline-flex" onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}
    onFocus={() => setFocus(true)} onBlur={() => setFocus(false)}
    onKeyDown={(event) => { if (event.key === 'Escape') { setHover(false); setFocus(false) } }}>
    {children({ 'aria-describedby': id })}
    <span id={id} role="tooltip" style={style} hidden={!visible}
      className="z-30 whitespace-normal break-words rounded-lg bg-slate-900 px-3 py-2 text-xs font-normal leading-5 text-white shadow-lg">{texto}</span>
  </span>
}
