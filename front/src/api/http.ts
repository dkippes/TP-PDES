export const apiUrl = (import.meta.env?.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '')

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'
export type JsonRequest = <T>(method: HttpMethod, path: string, body?: object) => Promise<T>

export const requestJson: JsonRequest = async <T>(method: HttpMethod, path: string, body?: object): Promise<T> =>
  readJson<T>(await sendJson(method, path, body))

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export function sendJson(method: HttpMethod, path: string, body?: object, accessToken?: string): Promise<Response> {
  return fetch(`${apiUrl}/api/${path}`, {
    method,
    credentials: 'include',
    headers: {
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
}

export async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.json().catch(() => null) as {
      message?: string
      fieldErrors?: Record<string, string>
    } | null
    const fields = Object.entries(error?.fieldErrors ?? {}).map(([field, message]) => `${field}: ${message}`)
    throw new ApiError(response.status, fields.length > 0
      ? fields.join('. ')
      : error?.message ?? 'No se pudo completar la operación.')
  }
  // Logout and DELETE can succeed without a JSON response body.
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}
