import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createPaquetesApi, requestJson } from './api-modules.mjs'

test('public catalog requests every package without credentials or filters in the URL', async (t) => {
  const paquetes = Array.from({ length: 12 }, (_, index) => ({
    id: index + 1, origen: 'Buenos Aires', destino: `Destino ${index + 1}`, hotel: { nombre: 'Hotel Demo', pais: 'España' },
    disponibilidad: index % 3 === 0 ? null : index,
  }))
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(new URL(url).pathname, '/api/paquetes')
    assert.equal(new URL(url).search, '')
    assert.equal(options.method, 'GET')
    assert.equal(options.headers.Authorization, undefined)
    return new Response(JSON.stringify(paquetes))
  })
  const recibidos = await createPaquetesApi(requestJson).listar()
  assert.deepEqual(recibidos, paquetes)
  // Unknown availability (null) must reach the UI as null, not be coerced to 0 or dropped.
  assert.equal(recibidos[0].disponibilidad, null)
  assert.equal(recibidos[1].disponibilidad, 1)
})

test('buscar serializes filters, omits empty values and sends no credentials', async (t) => {
  const pagina = { contenido: [], pagina: 0, tamanio: 12, totalElementos: 0, totalPaginas: 0 }
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    const { pathname, searchParams } = new URL(url)
    assert.equal(pathname, '/api/paquetes/buscar')
    assert.deepEqual(Object.fromEntries(searchParams), { q: 'sol', destino: 'Madrid', precioMin: '0', agenciaId: '3', orden: 'PRECIO_ASC' })
    assert.equal(options.method, 'GET')
    assert.equal(options.headers.Authorization, undefined)
    return new Response(JSON.stringify(pagina))
  })
  const filtros = { q: ' sol ', destino: 'Madrid', origen: '  ', fechaDesde: '', precioMin: 0, precioMax: undefined, agenciaId: 3, orden: 'PRECIO_ASC' }
  assert.deepEqual(await createPaquetesApi(requestJson).buscar(filtros), pagina)
})

test('buscar without filters adds no query string', async (t) => {
  t.mock.method(globalThis, 'fetch', async (url) => {
    assert.equal(new URL(url).pathname, '/api/paquetes/buscar')
    assert.equal(new URL(url).search, '')
    return new Response('{}')
  })
  await createPaquetesApi(requestJson).buscar({})
})

test('listarAgencias requests the public agency list', async (t) => {
  const agencias = [{ id: 1, nombre: 'Viajes del Sur', email: 'a@b.c' }]
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(new URL(url).pathname, '/api/agencias')
    assert.equal(options.headers.Authorization, undefined)
    return new Response(JSON.stringify(agencias))
  })
  assert.deepEqual(await createPaquetesApi(requestJson).listarAgencias(), agencias)
})

test('package detail requests its own public endpoint and preserves the hotel photo', async (t) => {
  const paquete = { id: 8, origen: 'Buenos Aires', destino: 'Madrid', hotel: { nombre: 'Hotel Sol', foto: '/sol.jpg' }, disponibilidad: 4 }
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(new URL(url).pathname, '/api/paquetes/8')
    assert.equal(options.method, 'GET')
    assert.equal(options.headers.Authorization, undefined)
    return new Response(JSON.stringify(paquete))
  })
  const recibido = await createPaquetesApi(requestJson).obtener(8)
  assert.deepEqual(recibido, paquete)
  assert.equal(recibido.disponibilidad, 4)
})

test('package detail preserves not-found and service errors for the page', async (t) => {
  const mock = t.mock.method(globalThis, 'fetch', async () => new Response('{}', { status: 404 }))
  await assert.rejects(createPaquetesApi(requestJson).obtener(8), (error) => error.status === 404)
  mock.mock.mockImplementation(async () => new Response('{}', { status: 503 }))
  await assert.rejects(createPaquetesApi(requestJson).obtener(8), (error) => error.status === 503)
})
