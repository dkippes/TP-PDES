import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createAuthenticatedRequest, createValoracionesApi, createValoracionesPublicasApi, requestJson } from './api-modules.mjs'

const events = { onSessionRenewed: () => assert.fail('Unexpected renewal'), onSessionExpired: () => assert.fail('Unexpected expiry') }

test('public reviews need no token and preserve a zero average and anonymous content', async (t) => {
  const resultado = { promedio: 0, total: 1, valoraciones: [{ id: 1, puntaje: 0, comentario: null, fecha: '2026-10-25T12:00:00Z' }] }
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(new URL(url).pathname, '/api/paquetes/7/valoraciones')
    assert.equal(options.method, 'GET')
    assert.equal(options.headers.Authorization, undefined)
    return Response.json(resultado)
  })
  assert.deepEqual(await createValoracionesPublicasApi(requestJson).listar(7), resultado)
})

test('review operations use authenticated transport and the purchase id, independently from buying', async (t) => {
  const requests = []
  const propia = { compraId: 4, valoracion: { id: 9, puntaje: 10, comentario: null, fecha: '2026-10-25T12:00:00Z' } }
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(options.headers.Authorization, 'Bearer buyer')
    requests.push([options.method, new URL(url).pathname, options.body ? JSON.parse(options.body) : undefined])
    if (options.method === 'DELETE') return new Response(null, { status: 204 })
    return Response.json(options.method === 'GET' ? [propia] : propia)
  })
  const api = createValoracionesApi(createAuthenticatedRequest('buyer', events))
  assert.deepEqual(await api.listarPropias(), [propia])
  assert.deepEqual(await api.guardar(4, { puntaje: 10 }), propia)
  await api.eliminar(4)
  assert.deepEqual(requests, [
    ['GET', '/api/valoraciones/mias', undefined],
    ['PUT', '/api/compras/4/valoracion', { puntaje: 10 }],
    ['DELETE', '/api/compras/4/valoracion', undefined],
  ])
})

test('a failed rating preserves the status and message for retry without purchasing again', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => Response.json({ message: 'Compra no encontrada' }, { status: 404 }))
  const api = createValoracionesApi(createAuthenticatedRequest('buyer', events))
  await assert.rejects(api.guardar(4, { puntaje: 5, comentario: 'Viaje' }), { status: 404, message: 'Compra no encontrada' })
})
