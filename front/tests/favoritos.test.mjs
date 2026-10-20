import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createAuthenticatedRequest, createFavoritosApi, session } from './api-modules.mjs'

const user = { id: 1, nombre: 'Ana', apellido: 'Pérez', correo: 'ana@example.com', direccion: 'Quilmes', role: 'COMPRADOR' }

test('favorites list current package cards in server order and use the authenticated transport', async (t) => {
  const paquetes = [{ id: 8, destino: 'Madrid' }, { id: 3, destino: 'Bariloche' }]
  const requests = []
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    requests.push([new URL(url).pathname, options.method])
    assert.equal(options.headers.Authorization, 'Bearer buyer-token')
    assert.equal(options.credentials, 'include')
    assert.equal(options.body, undefined)
    if (options.method === 'GET') return new Response(JSON.stringify(paquetes.map((paquete) => ({ paquete, agregadoEn: '2026-10-19T12:00:00Z' }))))
    return new Response(null, { status: 204 })
  })
  const api = createFavoritosApi(createAuthenticatedRequest('buyer-token', {
    onSessionRenewed: () => assert.fail('Unexpected renewal'), onSessionExpired: () => assert.fail('Unexpected expiry'),
  }))

  assert.deepEqual(await api.listar(), paquetes)
  assert.equal(await api.guardar(8), undefined)
  assert.equal(await api.quitar(8), undefined)
  assert.deepEqual(requests, [['/api/favoritos', 'GET'], ['/api/favoritos/8', 'PUT'], ['/api/favoritos/8', 'DELETE']])
})

test('favorites retry with the renewed session after a 401', async (t) => {
  const renewed = { accessToken: 'new-token', expiresIn: 900, user }
  let notified
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    if (new URL(url).pathname === '/api/auth/refresh') return new Response(JSON.stringify(renewed))
    if (options.headers.Authorization === 'Bearer old-token') return new Response('{}', { status: 401 })
    assert.equal(options.headers.Authorization, 'Bearer new-token')
    return new Response(null, { status: 204 })
  })
  const api = createFavoritosApi(createAuthenticatedRequest('old-token', {
    onSessionRenewed: (value) => { notified = value }, onSessionExpired: () => assert.fail('Unexpected expiry'),
  }))
  await api.guardar(3)
  assert.deepEqual(notified, renewed)
  await session.restorePendingRefresh()
})

test('favorites expose backend failures so the UI can preserve the current state', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({ message: 'Paquete eliminado' }), { status: 404 }))
  const api = createFavoritosApi(createAuthenticatedRequest('token', { onSessionRenewed: () => {}, onSessionExpired: () => {} }))
  await assert.rejects(api.guardar(99), { status: 404, message: 'Paquete eliminado' })
  await assert.rejects(api.quitar(99), { status: 404, message: 'Paquete eliminado' })
  await assert.rejects(api.listar(), { status: 404, message: 'Paquete eliminado' })
})
