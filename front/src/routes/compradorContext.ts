import { useOutletContext } from 'react-router'
import type { useCompras } from '../hooks/useCompras'
import type { CompraControls } from '../types/compra'
import type { FavoritosControls } from '../types/favorito'
import type { PaqueteResponseDTO } from '../types/paquete'

/** Buyer state that `CompradorPage` shares with the catalog, favorites and purchases routes. */
export type CompradorContext = {
  favoritos: FavoritosControls
  paquetesFavoritos: PaqueteResponseDTO[]
  compra: CompraControls
  historial: ReturnType<typeof useCompras>
}

/** Undefined for visitors and other roles, whose catalog routes render without buyer actions. */
export function useCompradorContext(): CompradorContext | undefined {
  return useOutletContext<CompradorContext | null | undefined>() ?? undefined
}
