import type { ReactNode } from 'react'
import { secondaryClass } from './controls'

type AdminPanelContentProps = {
  loading: boolean
  loadingMessage: string
  error: string
  message: string
  onRetry: () => void
  children: ReactNode
}

export function AdminPanelContent({ loading, loadingMessage, error, message, onRetry, children }: AdminPanelContentProps) {
  return <>
    {message && !loading && <p role="status" className="mb-4 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
    {loading ? <p role="status" className="py-10 text-center text-slate-500">{loadingMessage}</p>
      : error ? <div className="py-6"><p role="alert" className="mb-4 text-sm text-red-700">{error}</p><button type="button" className={secondaryClass} onClick={onRetry}>Reintentar</button></div>
        : children}
  </>
}
