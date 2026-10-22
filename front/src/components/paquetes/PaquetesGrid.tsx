import type { CompraControls } from '../../types/compra'
import type { FavoritosControls } from '../../types/favorito'
import type { PaqueteResponseDTO } from '../../types/paquete'
import { disponibilidadVigente } from './disponibilidad'
import { PaqueteCard } from './PaqueteCard'

type PaquetesGridProps = {
  paquetes: PaqueteResponseDTO[]
  onVerDetalle: (id: number) => void
  favoritos?: FavoritosControls
  compra?: CompraControls
  className?: string
}

export function PaquetesGrid({ paquetes, onVerDetalle, favoritos, compra, className = 'grid gap-6 sm:grid-cols-2 lg:grid-cols-3' }: PaquetesGridProps) {
  return <div className={className}>
    {paquetes.map((paquete) => <PaqueteCard key={paquete.id} paquete={paquete} onVerDetalle={onVerDetalle} favorito={favoritos && {
      guardado: favoritos.ids.has(paquete.id), disabled: favoritos.disabled,
      onToggle: () => favoritos.toggle(paquete),
    }} onComprar={compra && (() => compra.comprar(paquete))}
    comprado={compra?.comprados.get(paquete.id)}
    disponibilidad={disponibilidadVigente(paquete, compra?.disponibilidades)} />)}
  </div>
}
