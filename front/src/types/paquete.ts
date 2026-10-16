// Fields consumed from GET /api/paquetes; the API also returns price, dates and flight IDs.
export type PaqueteResponseDTO = {
  id: number
  origen: string
  destino: string
  hotel: { nombre: string }
}
