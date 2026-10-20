import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createAuthenticatedRequest, createComprasApi } from './api-modules.mjs'

const events = { onSessionRenewed: () => assert.fail('Unexpected renewal'), onSessionExpired: () => assert.fail('Unexpected expiry') }
const compra = { id: 5, operacionId: 'op-1', precio: 1500, fecha: '2026-10-20', paquete: { id: 8, destino: 'Madrid' } }

test('a purchase posts the package and its operation id with the authenticated transport', async (t) => {
  const requests = []
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    requests.push([new URL(url).pathname, options.method, JSON.parse(options.body)])
    assert.equal(options.headers.Authorization, 'Bearer buyer-token')
    assert.equal(options.headers['Content-Type'], 'application/json')
    return new Response(JSON.stringify(compra), { status: 201 })
  })
  const api = createComprasApi(createAuthenticatedRequest('buyer-token', events), () => 'op-1')

  const intento = api.iniciar(8)
  assert.equal(intento.operacionId, 'op-1')
  assert.deepEqual(await intento.confirmar(), compra)
  assert.deepEqual(requests, [['/api/compras', 'POST', { paqueteId: 8, operacionId: 'op-1' }]])
})

test('retrying an attempt reuses its operation id and a new attempt gets another one', async (t) => {
  const operaciones = []
  let fallar = true
  t.mock.method(globalThis, 'fetch', async (_url, options) => {
    operaciones.push(JSON.parse(options.body).operacionId)
    if (fallar) {
      fallar = false
      return new Response(JSON.stringify({ message: 'El servicio de vuelos no está disponible' }), { status: 503 })
    }
    return new Response(JSON.stringify(compra), { status: 200 })
  })
  let siguiente = 0
  const api = createComprasApi(createAuthenticatedRequest('token', events), () => `op-${++siguiente}`)

  const intento = api.iniciar(8)
  await assert.rejects(intento.confirmar(), { status: 503, message: 'El servicio de vuelos no está disponible' })
  assert.deepEqual(await intento.confirmar(), compra)
  await api.iniciar(8).confirmar()
  assert.deepEqual(operaciones, ['op-1', 'op-1', 'op-2'])
})

test('purchase rejections keep the backend status for the UI', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({ message: 'Sin lugar' }), { status: 409 }))
  const api = createComprasApi(createAuthenticatedRequest('token', events), () => 'op-1')
  await assert.rejects(api.iniciar(8).confirmar(), { status: 409, message: 'Sin lugar' })
})
