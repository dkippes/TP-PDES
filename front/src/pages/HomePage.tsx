import type { PaquetesApi } from '../api/paquetes'
import { GuestInvitation } from '../components/home/GuestInvitation'
import { PaquetesCatalog } from '../components/paquetes/PaquetesCatalog'
import { PaquetesSearchBar } from '../components/paquetes/PaquetesSearchBar'
import { usePaquetesCatalogo } from '../hooks/usePaquetesCatalogo'
import type { PaqueteBusquedaDTO } from '../types/paquete'

type HomePageProps = {
  api: PaquetesApi
  isAuthenticated: boolean
  onRegister: () => void
  onLogin: () => void
  onSearch: (filtros: PaqueteBusquedaDTO) => void
}

export function HomePage({ api, isAuthenticated, onRegister, onLogin, onSearch }: HomePageProps) {
  const { data, loading, error, reload, sugerencias } = usePaquetesCatalogo(api)
  return <div className="space-y-8">
    <PaquetesSearchBar sugerencias={sugerencias} onSearch={onSearch} />
    {!isAuthenticated && <GuestInvitation onRegister={onRegister} onLogin={onLogin} />}
    <PaquetesCatalog paquetes={data} loading={loading} error={error} onRetry={reload} />
  </div>
}
