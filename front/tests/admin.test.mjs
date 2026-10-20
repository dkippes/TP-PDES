import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createAdminApi, createAuthenticatedRequest } from './api-modules.mjs'
const json = (body, status = 200) => new Response(JSON.stringify(body), { status })
const session = { accessToken: 'renewed', user: { id: 1, role: 'ADMINISTRADOR' }, expiresIn: 900, tokenType: 'Bearer' }
const noop = () => {}
const api = () => createAdminApi(createAuthenticatedRequest('original', { onSessionRenewed: noop, onSessionExpired: noop }))

test('user and agency CRUD sends signed requests with the backend field names and accepts empty deletes', async (t) => {
  const requests = []
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    requests.push({ path: new URL(url).pathname, ...options })
    return options.method === 'DELETE' ? new Response(null, { status: 204 }) : json({ id: 7 })
  })
  const client = api()
  const usuario = { nombre: 'Ana', apellido: 'Pérez', correo: 'ana@example.com', direccion: 'Quilmes', rol: 'AGENTE', agenciaId: 2, password: '12345678' }
  const { password: _password, ...updated } = { ...usuario, rol: 'COMPRADOR', agenciaId: null }
  const agencia = { nombre: 'Viajes', email: 'viajes@example.com' }
  await client.usuarios.listar()
  await client.usuarios.crear(usuario)
  await client.usuarios.editar(7, updated)
  assert.equal(await client.usuarios.eliminar(7), undefined)
  await client.agencias.listar()
  await client.agencias.crear(agencia)
  await client.agencias.editar(8, agencia)
  assert.equal(await client.agencias.eliminar(8), undefined)
  assert.deepEqual(requests.map(({ method, path }) => [method, path]), [
    ['GET', '/api/usuarios'], ['POST', '/api/usuarios'], ['PUT', '/api/usuarios/7'], ['DELETE', '/api/usuarios/7'],
    ['GET', '/api/agencias'], ['POST', '/api/agencias'], ['PUT', '/api/agencias/8'], ['DELETE', '/api/agencias/8'],
  ])
  for (const request of requests) {
    assert.equal(request.headers.Authorization, 'Bearer original')
    assert.equal(request.credentials, 'include')
  }
  assert.deepEqual(JSON.parse(requests[1].body), usuario)
  assert.deepEqual(JSON.parse(requests[2].body), updated)
  assert.equal('password' in JSON.parse(requests[2].body), false)
  assert.deepEqual(JSON.parse(requests[5].body), agencia)
})

test('concurrent expired requests share one refresh and retry with the renewed access token', async (t) => {
  let refreshes = 0
  let resolveRefresh
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    if (url.endsWith('/auth/refresh')) {
      refreshes++
      return new Promise((resolve) => { resolveRefresh = resolve })
    }
    return options.headers.Authorization === 'Bearer renewed' ? json([]) : json({}, 401)
  })
  const renewed = []
  const client = createAdminApi(createAuthenticatedRequest('expired', { onSessionRenewed: (value) => renewed.push(value), onSessionExpired: () => assert.fail('session should renew') }))
  const pending = Promise.all([client.usuarios.listar(), client.agencias.listar()])
  // Let both unauthorized responses reach the shared refresh before completing it.
  await new Promise((resolve) => setImmediate(resolve))
  assert.equal(refreshes, 1)
  resolveRefresh(json(session))
  assert.deepEqual(await pending, [[], []])
  assert.equal(renewed.length, 2)
  assert.deepEqual(renewed[0], session)
})

test('an invalid refresh expires the session while a temporary refresh failure preserves it', async (t) => {
  let expired = 0
  let refreshStatus = 503
  t.mock.method(globalThis, 'fetch', async (url) => json({ message: 'Refresh failed' }, url.endsWith('/refresh') ? refreshStatus : 401))
  const client = createAdminApi(createAuthenticatedRequest('expired', { onSessionRenewed: noop, onSessionExpired: () => { expired++ } }))
  await assert.rejects(client.usuarios.listar(), (error) => error.status === 503)
  assert.equal(expired, 0)
  refreshStatus = 401
  await assert.rejects(client.usuarios.listar(), (error) => error.status === 401)
  assert.equal(expired, 1)
})

test('validation, conflicts and forbidden responses keep their backend errors without retrying mutations', async (t) => {
  const mock = t.mock.method(globalThis, 'fetch', async () => json({ message: 'La agencia tiene referencias' }, 409))
  await assert.rejects(api().agencias.eliminar(1), (error) => error.status === 409 && error.message === 'La agencia tiene referencias')
  assert.equal(mock.mock.callCount(), 1)
  mock.mock.mockImplementation(async () => json({ fieldErrors: { correo: 'Email inválido' } }, 400))
  await assert.rejects(api().usuarios.crear({}), /correo: Email inválido/)
  mock.mock.mockImplementation(async () => json({ message: 'Access is denied' }, 403))
  await assert.rejects(api().usuarios.listar(), (error) => error.status === 403)
  mock.mock.mockImplementation(async () => new Response('Unavailable', { status: 503 }))
  await assert.rejects(api().agencias.listar(), (error) => error.status === 503)
})
