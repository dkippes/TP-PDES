import type { PaqueteResponseDTO } from './paquete'

export type FavoritoResponseDTO = {
  paquete: PaqueteResponseDTO
  agregadoEn: string
}

export type FavoritosControls = {
  ids: ReadonlySet<number>
  disabled: boolean
  loading: boolean
  busy: boolean
  error: string
  actionError: string
  toggle: (paquete: PaqueteResponseDTO) => void
  reload: () => void
}
