import { useCallback, useEffect, useState } from 'react'
import type { View } from '../types'
import type { PaqueteBusquedaDTO } from '../types/paquete'

type Navigation = {
  view: View
  busqueda: PaqueteBusquedaDTO
  paqueteId: number | null
  desdeListado: boolean
}

function readNavigation(): Navigation {
  const saved = window.history.state?.navigation as Navigation | undefined
  if (window.location.pathname.startsWith('/paquetes/')) {
    const match = /^\/paquetes\/([1-9]\d*)\/?$/.exec(window.location.pathname)
    const id = match ? Number(match[1]) : NaN
    return {
      view: 'paquete', paqueteId: Number.isSafeInteger(id) ? id : null,
      busqueda: saved?.busqueda ?? {}, desdeListado: saved?.desdeListado ?? false,
    }
  }
  return saved ?? { view: 'home', busqueda: {}, paqueteId: null, desdeListado: false }
}

/** The detail has a shareable URL; history also preserves the source list and its filters. */
export function useNavigation() {
  const [navigation, setNavigation] = useState(readNavigation)

  useEffect(() => {
    window.history.replaceState({ navigation: readNavigation() }, '', window.location.href)
    const onPopState = () => { setNavigation(readNavigation()); window.scrollTo(0, 0) }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  const go = useCallback((next: Navigation, replace = false) => {
    const url = next.view === 'paquete' ? `/paquetes/${next.paqueteId}` : '/'
    if (replace) window.history.replaceState({ navigation: next }, '', url)
    else window.history.pushState({ navigation: next }, '', url)
    setNavigation(next)
    window.scrollTo(0, 0)
  }, [])

  const navigate = useCallback((view: View) => {
    go({ ...navigation, view, paqueteId: null, desdeListado: false })
  }, [go, navigation])

  const search = (busqueda: PaqueteBusquedaDTO) => go({ ...navigation, view: 'resultados', busqueda, paqueteId: null })
  const changeSearch = (busqueda: PaqueteBusquedaDTO) => go({ ...navigation, busqueda }, true)
  const openPackage = (paqueteId: number) => {
    go({ ...navigation, view: 'paquete', paqueteId, desdeListado: true })
  }
  const backToList = () => {
    if (navigation.desdeListado) window.history.back()
    else navigate('home')
  }

  return { ...navigation, navigate, search, changeSearch, openPackage, backToList }
}
