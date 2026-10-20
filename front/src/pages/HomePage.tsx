import type { PaquetesApi } from '../api/paquetes'
import { GuestInvitation, type GuestInvitationProps } from '../components/home/GuestInvitation'
import { FavoritosStatus } from '../components/paquetes/FavoritosStatus'
import { PaquetesCatalog } from '../components/paquetes/PaquetesCatalog'
import { PaquetesSearchBar } from '../components/paquetes/PaquetesSearchBar'
import { usePaquetesCatalogo } from '../hooks/usePaquetesCatalogo'
import type { FavoritosControls } from '../types/favorito'
import type { PaqueteBusquedaDTO, PaqueteResponseDTO } from '../types/paquete'

type HomePageProps = {
  api: PaquetesApi
  onSearch: (filtros: PaqueteBusquedaDTO) => void
  /** Shown to visitors only. */
  invitacion?: GuestInvitationProps
  /** Present for buyers, who can save packages as favorites. */
  favoritos?: FavoritosControls
  /** Present for buyers, who can purchase packages. */
  onComprar?: (paquete: PaqueteResponseDTO) => void
}

export function HomePage({ api, onSearch, invitacion, favoritos, onComprar }: Readonly<HomePageProps>) {
  const { data, loading, error, reload, sugerencias } = usePaquetesCatalogo(api)
  return <div className="space-y-8">
    <PaquetesSearchBar sugerencias={sugerencias} onSearch={onSearch} />
    {invitacion && <GuestInvitation {...invitacion} />}
    {favoritos && <FavoritosStatus favoritos={favoritos} />}
    <PaquetesCatalog paquetes={data} loading={loading} error={error} onRetry={reload} favoritos={favoritos} onComprar={onComprar} />
  </div>
}
