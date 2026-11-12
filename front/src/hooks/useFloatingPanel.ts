import { useLayoutEffect, useState, type CSSProperties, type RefObject } from 'react'

const MARGEN = 8

/**
 * Fixed position for a panel anchored to [anchor]: it opens below, or above when there is more room there, and is
 * limited to the visible space. Being fixed it neither grows nor scrolls the dialog that contains it.
 */
export function useFloatingPanel(anchor: RefObject<HTMLElement | null>, open: boolean, anchoMinimo = 0): CSSProperties | undefined {
  const [style, setStyle] = useState<CSSProperties>()

  useLayoutEffect(() => {
    if (!open) return
    const place = () => {
      const rect = anchor.current?.getBoundingClientRect()
      if (!rect) return
      const abajo = window.innerHeight - rect.bottom - MARGEN
      const arriba = rect.top - MARGEN
      const haciaArriba = abajo < 240 && arriba > abajo
      const ancho = Math.min(Math.max(rect.width, anchoMinimo), window.innerWidth - 2 * MARGEN)
      setStyle({
        position: 'fixed',
        left: Math.min(Math.max(MARGEN, rect.left), window.innerWidth - ancho - MARGEN),
        width: ancho,
        maxHeight: Math.max(160, haciaArriba ? arriba : abajo),
        ...(haciaArriba ? { bottom: window.innerHeight - rect.top + 4 } : { top: rect.bottom + 4 }),
      })
    }
    place()
    window.addEventListener('resize', place)
    // Capture: the dialog scrolls, not the window.
    window.addEventListener('scroll', place, true)
    return () => {
      window.removeEventListener('resize', place)
      window.removeEventListener('scroll', place, true)
    }
  }, [anchor, open, anchoMinimo])

  return open ? style : undefined
}
