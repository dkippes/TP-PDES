import { useState } from 'react'

export function useAsyncAction(fallbackMessage: string) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const execute = async (action: () => Promise<void>) => {
    setBusy(true)
    setError('')
    try {
      await action()
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : fallbackMessage)
    } finally {
      setBusy(false)
    }
  }

  return { busy, error, execute }
}
