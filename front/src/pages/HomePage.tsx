import type { PaquetesApi } from '../api/paquetes'
import { GuestInvitation } from '../components/home/GuestInvitation'
import { PaquetesCatalog } from '../components/paquetes/PaquetesCatalog'

type HomePageProps = {
  api: PaquetesApi
  isAuthenticated: boolean
  onRegister: () => void
  onLogin: () => void
}

export function HomePage({ api, isAuthenticated, onRegister, onLogin }: HomePageProps) {
  return <div className="space-y-8">
    {!isAuthenticated && <GuestInvitation onRegister={onRegister} onLogin={onLogin} />}
    <PaquetesCatalog api={api} />
  </div>
}
