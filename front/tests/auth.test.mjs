import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import ts from 'typescript'

// Exercise the actual TypeScript modules without adding a browser test framework.
const source = await readFile(new URL('../src/api/auth.ts', import.meta.url), 'utf8')
const compile = (text) => ts.transpileModule(text, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 },
}).outputText
const apiUrl = 'data:text/javascript;base64,' + Buffer.from(compile(source)).toString('base64')
const auth = await import(apiUrl)
const sessionSource = await readFile(new URL('../src/auth/session.ts', import.meta.url), 'utf8')
const session = await import('data:text/javascript;base64,' + Buffer.from(
  compile(sessionSource).replace("'../api/auth'", JSON.stringify(apiUrl)),
).toString('base64'))
const user = { id: 1, nombre: 'Diego', apellido: 'K', correo: 'diego@example.com', direccion: 'Quilmes', role: 'COMPRADOR' }
const login = { user, accessToken: 'token', tokenType: 'Bearer', expiresIn: 900 }
const json = (body, status = 200) => new Response(JSON.stringify(body), { status })

test('register and login use the existing contracts and include cookies', async (t) => {
  const requests = []
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    requests.push({ url, options })
    return json(url.endsWith('/register') ? user : login)
  })
  const registration = { nombre: 'Diego', apellido: 'K', correo: user.correo, direccion: user.direccion, password: 'password123' }
  assert.deepEqual(await auth.registrarUsuario(registration), user)
  assert.deepEqual(await auth.iniciarSesion(user.correo, registration.password), login)
  assert.ok(requests[0].url.endsWith('/api/auth/register'))
  assert.ok(requests[1].url.endsWith('/api/auth/login'))
  assert.deepEqual(JSON.parse(requests[0].options.body), registration)
  for (const request of requests) assert.equal(request.options.credentials, 'include')
})

test('logout accepts an empty successful body and revokes the cookie session', async (t) => {
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.ok(url.endsWith('/api/auth/logout'))
    assert.equal(options.credentials, 'include')
    assert.equal(options.body, undefined)
    return new Response(null, { status: 200 })
  })
  assert.equal(await auth.cerrarSesion(), undefined)
})

test('backend error messages, validation errors and non-JSON failures are surfaced', async (t) => {
  const mock = t.mock.method(globalThis, 'fetch', async () => json({ message: 'Invalid credentials' }, 401))
  await assert.rejects(auth.iniciarSesion('bad@example.com', 'password123'), (error) =>
    error instanceof auth.AuthError && error.status === 401 && error.message === 'Invalid credentials')
  mock.mock.mockImplementation(async () => json({ fieldErrors: { password: 'must be at least 8 characters' } }, 400))
  await assert.rejects(auth.registrarUsuario({}), /password: must be at least 8 characters/)
  mock.mock.mockImplementation(async () => new Response('Unavailable', { status: 503 }))
  await assert.rejects(auth.renovarSesion(), (error) => error.status === 503)
})

test('concurrent restores share one rotating refresh and logout waits for it', async (t) => {
  let complete
  const mock = t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.ok(url.endsWith('/api/auth/refresh'))
    assert.equal(options.credentials, 'include')
    return new Promise((resolve) => { complete = resolve })
  })
  const first = session.restoreSession()
  const second = session.restoreSession()
  assert.equal(first, second)
  let finished = false
  const waiting = session.restorePendingRefresh().then(() => { finished = true })
  await Promise.resolve()
  assert.equal(finished, false)
  complete(json(login))
  assert.deepEqual(await first, login)
  await waiting
  assert.equal(mock.mock.callCount(), 1)
  mock.mock.mockImplementation(async () => json(login))
  await session.restoreSession()
  assert.equal(mock.mock.callCount(), 2)
})

test('failed refresh allows a later retry and does not block logout', async (t) => {
  const mock = t.mock.method(globalThis, 'fetch', async () => json({ message: 'Invalid refresh token' }, 401))
  await assert.rejects(session.restoreSession(), (error) => error.status === 401)
  await session.restorePendingRefresh()
  mock.mock.mockImplementation(async () => json(login))
  assert.deepEqual(await session.restoreSession(), login)
})
