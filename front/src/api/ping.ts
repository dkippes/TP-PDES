export type PingResponse = {
  service: string
  status: string
  timestamp: string
  flyingService?: Record<string, unknown>
  persisted?: { id: number; totalPings: number }
}

export const pingApiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export async function pingServices(): Promise<PingResponse> {
  const response = await fetch(`${pingApiUrl}/api/ping`, { method: 'POST' })
  // Keep the raw service response and HTTP status available in the diagnostic view.
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  return response.json() as Promise<PingResponse>
}
