import { useCallback, useEffect, useState } from 'react'

export function useAdminData<T>(load: () => Promise<T>, initial: T) {
  const [state, setState] = useState({ data: initial, loading: true, error: '' })
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let cancelled = false
    load().then((data) => {
      if (!cancelled) setState({ data, loading: false, error: '' })
    }).catch((error: unknown) => {
      if (!cancelled) setState((current) => ({
        ...current, loading: false, error: error instanceof Error ? error.message : 'No se pudieron cargar los datos.',
      }))
    })
    return () => { cancelled = true }
  }, [load, revision])

  const reload = useCallback(() => {
    setState((current) => ({ ...current, loading: true, error: '' }))
    setRevision((current) => current + 1)
  }, [])
  return { ...state, reload }
}
