import type { FavoritosControls } from '../../types/favorito'
import type { PaqueteResponseDTO } from '../../types/paquete'
import { PaqueteCard } from './PaqueteCard'

type PaquetesGridProps = {
  paquetes: PaqueteResponseDTO[]
  favoritos?: FavoritosControls
  className?: string
}

export function PaquetesGrid({ paquetes, favoritos, className = 'grid gap-6 sm:grid-cols-2 lg:grid-cols-3' }: PaquetesGridProps) {
  return <div className={className}>
    {paquetes.map((paquete) => <PaqueteCard key={paquete.id} paquete={paquete} favorito={favoritos && {
      guardado: favoritos.ids.has(paquete.id), disabled: favoritos.disabled,
      onToggle: () => favoritos.toggle(paquete),
    }} />)}
  </div>
}
