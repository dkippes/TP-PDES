import { useMemo } from 'react'
import { createAdminApi } from '../../api/admin'
import { createAuthenticatedRequest, type SessionEvents } from '../../api/authenticated'

export function useAdminApi(accessToken: string, { onSessionRenewed, onSessionExpired }: SessionEvents) {
  return useMemo(() => createAdminApi(createAuthenticatedRequest(accessToken, { onSessionRenewed, onSessionExpired })),
    [accessToken, onSessionRenewed, onSessionExpired])
}
