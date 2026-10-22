export const rutas = {
  inicio: '/',
  resultados: '/resultados',
  favoritos: '/favoritos',
  compras: '/compras',
  admin: '/admin',
  login: '/login',
  registro: '/registro',
  paquete: (id: number) => `/paquetes/${id}`,
} as const

/** Location state that login and registration carry so the visitor returns to the protected page afterwards. */
export type DestinoState = { from?: string }

export function destinoTrasLogin(state: unknown): string {
  const from = (state as DestinoState | null)?.from
  // Only same-app paths: a crafted state must not redirect elsewhere.
  const valido = typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') && !esRutaDeVisitante(from)
  return valido ? from : rutas.inicio
}

export function esRutaDeVisitante(path: string): boolean {
  return path === rutas.login || path === rutas.registro
}
