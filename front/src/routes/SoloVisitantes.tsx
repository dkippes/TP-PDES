import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import type { SesionAutenticada } from '../api/auth'
import { destinoTrasLogin } from './rutas'

type SoloVisitantesProps = { session: SesionAutenticada | null; children: ReactNode }

/** Login and registration are for visitors; a session (including one just started) continues to its destination. */
export function SoloVisitantes({ session, children }: Readonly<SoloVisitantesProps>) {
  const location = useLocation()
  if (session) return <Navigate to={destinoTrasLogin(location.state)} replace />
  return children
}
