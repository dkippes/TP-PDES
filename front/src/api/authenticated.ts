import type { SesionAutenticada } from './auth'
import { ApiError, readJson, sendJson, type HttpMethod } from './http'
import { restoreSession } from '../auth/session'

export type SessionEvents = {
  onSessionRenewed: (session: SesionAutenticada) => void
  onSessionExpired: () => void
}

export type AuthenticatedRequest = <T>(method: HttpMethod, path: string, body?: object) => Promise<T>

export function createAuthenticatedRequest(accessToken: string, events: SessionEvents): AuthenticatedRequest {
  return async function request<T>(method: HttpMethod, path: string, body?: object): Promise<T> {
    let response = await sendJson(method, path, body, accessToken)
    if (response.status === 401) {
      // Share the rotating refresh with App's timer and concurrent requests.
      let session: SesionAutenticada
      try {
        session = await restoreSession()
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) events.onSessionExpired()
        throw error
      }
      events.onSessionRenewed(session)
      response = await sendJson(method, path, body, session.accessToken)
    }
    if (response.status === 401) events.onSessionExpired()
    return readJson<T>(response)
  }
}
