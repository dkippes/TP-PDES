import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import type { SesionAutenticada } from '../api/auth'
import type { Rol } from '../types/admin'
import { rutas, type DestinoState } from './rutas'

type RequireRoleProps = {
  session: SesionAutenticada | null
  role: Rol
  children: (session: SesionAutenticada) => ReactNode
}

/** Visitors go to login and come back afterwards; accounts with another role go to Inicio. */
export function RequireRole({ session, role, children }: Readonly<RequireRoleProps>) {
  const location = useLocation()
  if (!session) return <Navigate to={rutas.login} replace state={{ from: location.pathname } satisfies DestinoState} />
  if (session.user.role !== role) return <Navigate to={rutas.inicio} replace />
  return children(session)
}
