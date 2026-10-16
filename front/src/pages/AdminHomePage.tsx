import { useState } from 'react'
import type { SesionAutenticada } from '../api/auth'
import { AdminHeader } from '../components/admin/AdminHeader'
import { AdminSections, type AdminSection } from '../components/admin/AdminSections'
import { AgenciasPanel } from '../components/admin/AgenciasPanel'
import { UsuariosPanel } from '../components/admin/UsuariosPanel'
import { useAdminApi } from '../hooks/admin/useAdminApi'
import type { UsuarioResponseDTO } from '../types/admin'

type Props = {
  session: SesionAutenticada
  onSessionRenewed: (session: SesionAutenticada) => void
  onSessionExpired: () => void
  onCurrentUserUpdated: (usuario: UsuarioResponseDTO, passwordChanged: boolean) => void
}

export function AdminHomePage({ session, onSessionRenewed, onSessionExpired, onCurrentUserUpdated }: Props) {
  const [section, setSection] = useState<AdminSection>('usuarios')
  const api = useAdminApi(session.accessToken, { onSessionRenewed, onSessionExpired })

  return <div className="space-y-6">
    <AdminHeader />
    <AdminSections section={section} onSelect={setSection} />
    {section === 'usuarios'
      ? <UsuariosPanel api={api.usuarios} agenciasApi={api.agencias} currentUserId={session.user.id} onCurrentUserUpdated={onCurrentUserUpdated} />
      : <AgenciasPanel api={api.agencias} />}
  </div>
}
