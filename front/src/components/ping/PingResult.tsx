import type { PingResponse } from '../../api/ping'

export function PingResult({ data }: { data: PingResponse }) {
  const flyingStatus = typeof data.flyingService?.status === 'string' ? data.flyingService.status : '?'

  return <>
    <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, marginTop: 12, textAlign: 'left', overflow: 'auto' }}>
      {JSON.stringify(data, null, 2)}
    </pre>
    <div style={{ marginTop: 12, textAlign: 'left' }}>
      <p>Backend: <strong>{data.status}</strong></p>
      <p>Flying: <strong>{flyingStatus}</strong></p>
      {data.persisted && <p>Guardado en DB: id <strong>{data.persisted.id}</strong> - total pings: <strong>{data.persisted.totalPings}</strong></p>}
    </div>
  </>
}
