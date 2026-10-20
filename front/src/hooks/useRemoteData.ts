import { useCallback, useEffect, useRef, useState } from 'react'

export function useRemoteData<T>(load: () => Promise<T>, initial: T) {
  const [state, setState] = useState({ data: initial, loading: true, error: '' })
  const [revision, setRevision] = useState(0)
  const dataVersion = useRef(0)
  useEffect(() => {
    let cancelled = false
    const version = ++dataVersion.current
    load().then((data) => {
      if (!cancelled && version === dataVersion.current) setState({ data, loading: false, error: '' })
    }).catch((error: unknown) => {
      if (!cancelled && version === dataVersion.current) setState((current) => ({
        ...current, loading: false, error: error instanceof Error ? error.message : 'No se pudieron cargar los datos.',
      }))
    })
    return () => { cancelled = true }
  }, [load, revision])

  const reload = useCallback(() => {
    setState((current) => ({ ...current, loading: true, error: '' }))
    setRevision((current) => current + 1)
  }, [])
  const updateData = useCallback((update: (data: T) => T) => {
    // Una lectura iniciada antes de una escritura confirmada no debe pisar ese cambio.
    dataVersion.current += 1
    setState((current) => ({ data: update(current.data), loading: false, error: '' }))
  }, [])
  return { ...state, reload, updateData }
}
