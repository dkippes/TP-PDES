import { renovarSesion, type SesionAutenticada } from '../api/auth'

let pendingRefresh: Promise<SesionAutenticada> | null = null

// Coalesce concurrent refresh calls, including React StrictMode's startup effects.
// Refresh tokens rotate on the server; replaying the same cookie is invalid.
export function restoreSession(): Promise<SesionAutenticada> {
  pendingRefresh ??= renovarSesion().finally(() => {
    pendingRefresh = null
  })
  return pendingRefresh
}

export async function restorePendingRefresh(): Promise<void> {
  await pendingRefresh?.catch(() => undefined)
}
