import { pingApiUrl } from '../api/ping'
import { usePing } from '../hooks/usePing'
import { PingResult } from './ping/PingResult'

export default function Ping() {
  const { data, loading, error, ping } = usePing()

  return (
    <div style={{ border: '1px solid #ccc', borderRadius: 8, padding: 16, maxWidth: 600 }}>
      <h2>Ping Chain: Front → Backend → Flying</h2>
      <p style={{ fontSize: 14, color: '#666' }}>
        Frontend llama a <code>{pingApiUrl}/api/ping</code> y el backend reenvía a flying-service
      </p>
      <button
        onClick={ping}
        disabled={loading}
        style={{ padding: '8px 16px', cursor: loading ? 'not-allowed' : 'pointer' }}
      >
        {loading ? 'Cargando...' : 'Probar Ping'}
      </button>
      {error && <p style={{ color: 'red' }}>Error: {error}</p>}
      {data && <PingResult data={data} />}
    </div>
  )
}
