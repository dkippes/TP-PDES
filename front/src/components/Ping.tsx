import { useState } from 'react'

type PingResponse = {
  service: string
  status: string
  timestamp: string
  flyingService?: Record<string, unknown>
  persisted?: { id: number; totalPings: number }
}

export default function Ping() {
  const [data, setData] = useState<PingResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'

  const handlePing = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(`${apiUrl}/api/ping`)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const json = await res.json()
      setData(json)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error desconocido')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ border: '1px solid #ccc', borderRadius: 8, padding: 16, maxWidth: 600 }}>
      <h2>Ping Chain: Front → Backend → Flying</h2>
      <p style={{ fontSize: 14, color: '#666' }}>
        Frontend llama a <code>{apiUrl}/api/ping</code> y el backend reenvía a flying-service
      </p>
      <button
        onClick={handlePing}
        disabled={loading}
        style={{ padding: '8px 16px', cursor: loading ? 'not-allowed' : 'pointer' }}
      >
        {loading ? 'Cargando...' : 'Probar Ping'}
      </button>

      {error && <p style={{ color: 'red' }}>Error: {error}</p>}

      {data && (
        <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, marginTop: 12, textAlign: 'left', overflow: 'auto' }}>
          {JSON.stringify(data, null, 2)}
        </pre>
      )}

      {data && (
        <div style={{ marginTop: 12, textAlign: 'left' }}>
          <p>Backend: <strong>{data.status}</strong></p>
          <p>Flying: <strong>{(data.flyingService as any)?.status ?? '?'}</strong></p>
          {data.persisted && (
            <p>Guardado en DB: id <strong>{data.persisted.id}</strong> - total pings: <strong>{data.persisted.totalPings}</strong></p>
          )}
        </div>
      )}
    </div>
  )
}
