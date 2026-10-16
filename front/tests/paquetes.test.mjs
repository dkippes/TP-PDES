import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createPaquetesApi, requestJson } from './api-modules.mjs'

test('public catalog requests every package without credentials or filters in the URL', async (t) => {
  const paquetes = Array.from({ length: 12 }, (_, index) => ({
    id: index + 1, origen: 'Buenos Aires', destino: `Destino ${index + 1}`, hotel: { nombre: 'Hotel Demo' },
  }))
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(new URL(url).pathname, '/api/paquetes')
    assert.equal(new URL(url).search, '')
    assert.equal(options.method, 'GET')
    assert.equal(options.headers.Authorization, undefined)
    return new Response(JSON.stringify(paquetes))
  })
  assert.deepEqual(await createPaquetesApi(requestJson).listar(), paquetes)
})
