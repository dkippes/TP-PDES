import { useState } from 'react'
import { pingServices, type PingResponse } from '../api/ping'
import { useAsyncAction } from './useAsyncAction'

export function usePing() {
  const [data, setData] = useState<PingResponse | null>(null)
  const { busy, error, execute } = useAsyncAction('Error desconocido')
  const ping = () => execute(async () => { setData(await pingServices()) })

  return { data, loading: busy, error, ping }
}
