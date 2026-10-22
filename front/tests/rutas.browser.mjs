import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { chromium, expect } from '@playwright/test'
import { createServer } from 'vite'

const baseURL = 'http://127.0.0.1:15176'
const paquete = {
  id: 1, origen: 'Buenos Aires', destino: 'Bariloche', precio: 1500, disponibilidad: 12,
  fechaInicial: '2027-12-01', fechaFinal: '2027-12-10',
  hotel: { nombre: 'Hotel del Lago', pais: 'Argentina', foto: '' },
  agencia: { id: 2, nombre: 'Viajes del Sur' },
}
const usuario = (role) => ({ id: 5, nombre: 'Ana', apellido: 'Pérez', correo: 'ana@example.com', direccion: 'Quilmes', role })
let server
let browser

before(async () => {
  server = await createServer({
    root: fileURLToPath(new URL('..', import.meta.url)),
    server: { host: '127.0.0.1', port: 15176, strictPort: true },
    define: { 'import.meta.env.VITE_API_URL': JSON.stringify(baseURL) },
  })
  await server.listen()
  browser = await chromium.launch({ channel: 'chrome', headless: true })
})

after(async () => {
  await browser?.close()
  await server?.close()
})

/** `role` is the session restored from the refresh cookie; `loginRole` is the account a login returns. */
async function openPage(t, { role, loginRole } = {}) {
  const context = await browser.newContext({ baseURL })
  const page = await context.newPage()
  page.setDefaultTimeout(8000)
  const errors = []
  // The refresh cookie is revoked by logout, like the backend does.
  let restored = role
  page.on('pageerror', (error) => errors.push(error.message))
  await page.route((url) => url.pathname.startsWith('/api/'), async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    const method = request.method()
    if (method === 'OPTIONS') return route.fulfill({ status: 204 })
    if (path === '/api/auth/refresh') return route.fulfill(restored
      ? { json: { accessToken: 'test-token', expiresIn: 900, user: usuario(restored) } }
      : { status: 401, json: {} })
    if (path === '/api/auth/login' && loginRole) return route.fulfill({
      json: { accessToken: 'test-token', tokenType: 'Bearer', expiresIn: 900, user: usuario(loginRole) },
    })
    if (path === '/api/auth/register') return route.fulfill({ status: 201, json: usuario('COMPRADOR') })
    if (path === '/api/auth/logout') {
      restored = undefined
      return route.fulfill({ status: 204 })
    }
    if (path === '/api/paquetes') return route.fulfill({ json: [paquete] })
    if (['/api/favoritos', '/api/compras', '/api/usuarios'].includes(path)) return route.fulfill({ json: [] })
    if (path === '/api/agencias') return route.fulfill({ json: [paquete.agencia] })
    errors.push(`Unexpected request: ${method} ${path}`)
    return route.fulfill({ status: 500, json: {} })
  })
  t.after(async () => { await context.close(); assert.deepEqual(errors, []) })
  return page
}

const nav = (page) => page.getByRole('navigation', { name: 'Navegación principal' })

async function login(page) {
  await page.getByLabel('Correo electrónico').fill('ana@example.com')
  await page.getByLabel('Contraseña').fill('password123')
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
}

test('each buyer page has its own URL that survives a reload and back navigation', async (t) => {
  const page = await openPage(t, { role: 'COMPRADOR' })
  await page.goto('/')
  for (const [boton, url, titulo] of [['Mis favoritos', '/favoritos', 'Mis favoritos'], ['Mis compras', '/compras', 'Mis compras'], ['Inicio', '/', 'Todos los paquetes']]) {
    await nav(page).getByRole('button', { name: boton, exact: true }).click()
    await expect(page).toHaveURL(`${baseURL}${url}`)
    await expect(nav(page).getByRole('button', { name: boton, exact: true })).toHaveAttribute('aria-current', 'page')
    await page.reload()
    await expect(page.getByRole('heading', { name: titulo, exact: true })).toBeVisible()
  }
  await page.goBack()
  await expect(page).toHaveURL(`${baseURL}/compras`)
  await expect(page.getByRole('heading', { name: 'Mis compras', exact: true })).toBeVisible()
})

test('a visitor asking for a protected page logs in and continues to it', async (t) => {
  const page = await openPage(t, { loginRole: 'COMPRADOR' })
  await page.goto('/compras')
  await expect(page).toHaveURL(`${baseURL}/login`)
  await login(page)
  await expect(page).toHaveURL(`${baseURL}/compras`)
  await expect(page.getByRole('heading', { name: 'Mis compras', exact: true })).toBeVisible()
})

test('the destination is kept when the visitor registers before logging in', async (t) => {
  const page = await openPage(t, { loginRole: 'COMPRADOR' })
  await page.goto('/favoritos')
  await page.getByRole('button', { name: 'Registrate', exact: true }).click()
  await expect(page).toHaveURL(`${baseURL}/registro`)
  await page.getByLabel('Nombre', { exact: true }).fill('Ana')
  await page.getByLabel('Apellido', { exact: true }).fill('Pérez')
  await page.getByLabel('Dirección').fill('Quilmes')
  await page.getByLabel('Correo electrónico').fill('ana@example.com')
  await page.getByLabel('Contraseña').fill('password123')
  await page.getByRole('button', { name: 'Registrarme', exact: true }).click()
  await expect(page).toHaveURL(`${baseURL}/login`)
  await login(page)
  await expect(page).toHaveURL(`${baseURL}/favoritos`)
  await expect(page.getByRole('heading', { name: 'Mis favoritos', exact: true })).toBeVisible()
})

test('a login whose role cannot open the requested page lands on Inicio', async (t) => {
  const page = await openPage(t, { loginRole: 'AGENTE' })
  await page.goto('/admin')
  await expect(page).toHaveURL(`${baseURL}/login`)
  await login(page)
  await expect(page).toHaveURL(`${baseURL}/`)
  await expect(page.getByRole('heading', { name: 'Todos los paquetes' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Administración', exact: true })).toHaveCount(0)
})

test('visitors are sent to login from every protected page', async (t) => {
  const page = await openPage(t)
  for (const path of ['/admin', '/favoritos', '/compras']) {
    await page.goto(path)
    await expect(page).toHaveURL(`${baseURL}/login`)
    await expect(page.getByRole('heading', { name: 'Iniciar sesión', exact: true })).toBeVisible()
  }
})

test('only the administrator opens administration', async (t) => {
  const admin = await openPage(t, { role: 'ADMINISTRADOR' })
  await admin.goto('/admin')
  await expect(admin.getByRole('heading', { name: 'Administración', exact: true })).toBeVisible()
  await expect(nav(admin).getByRole('button', { name: 'Administración', exact: true })).toHaveAttribute('aria-current', 'page')
  for (const role of ['COMPRADOR', 'AGENTE']) {
    const page = await openPage(t, { role })
    await page.goto('/admin')
    await expect(page).toHaveURL(`${baseURL}/`)
    await expect(page.getByRole('heading', { name: 'Todos los paquetes' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Administración', exact: true })).toHaveCount(0)
    await expect(nav(page).getByRole('button', { name: 'Administración', exact: true })).toHaveCount(0)
  }
})

test('only the buyer opens favorites and purchases', async (t) => {
  for (const role of ['ADMINISTRADOR', 'AGENTE']) {
    const page = await openPage(t, { role })
    for (const path of ['/favoritos', '/compras']) {
      await page.goto(path)
      await expect(page).toHaveURL(`${baseURL}/`)
      await expect(page.getByRole('heading', { name: 'Todos los paquetes' })).toBeVisible()
    }
    await expect(nav(page).getByRole('button', { name: /Mis (favoritos|compras)/ })).toHaveCount(0)
  }
})

test('signed-in accounts skip login and registration, and unknown URLs return to Inicio', async (t) => {
  const page = await openPage(t, { role: 'AGENTE' })
  for (const path of ['/login', '/registro', '/no-existe']) {
    await page.goto(path)
    await expect(page).toHaveURL(`${baseURL}/`)
    await expect(page.getByRole('heading', { name: 'Todos los paquetes' })).toBeVisible()
  }
})

test('logging out of a protected page returns to Inicio as a visitor', async (t) => {
  const page = await openPage(t, { role: 'ADMINISTRADOR' })
  await page.goto('/admin')
  await expect(page.getByRole('heading', { name: 'Administración', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click()
  await expect(page).toHaveURL(`${baseURL}/`)
  await expect(nav(page).getByRole('button', { name: 'Iniciar sesión', exact: true })).toBeVisible()
  await page.goto('/admin')
  await expect(page).toHaveURL(`${baseURL}/login`)
})

test('an expiry reported by concurrent requests keeps the page to return to', async (t) => {
  const page = await openPage(t, { role: 'COMPRADOR', loginRole: 'COMPRADOR' })
  let refreshes = 0
  let expirado = true
  // The session restores once; the token then stops working until a new login.
  await page.route('**/api/auth/refresh', (route) => (refreshes++ === 0
    ? route.fulfill({ json: { accessToken: 'viejo', expiresIn: 900, user: usuario('COMPRADOR') } })
    : route.fulfill({ status: 401, json: {} })))
  await page.route(/\/api\/(favoritos|compras)$/, (route) => expirado
    ? route.fulfill({ status: 401, json: {} })
    : route.fulfill({ json: [] }))
  await page.goto('/favoritos')
  await expect(page).toHaveURL(`${baseURL}/login`)
  await expect(page.getByText('Tu sesión terminó. Iniciá sesión nuevamente.')).toBeVisible()
  expirado = false
  await login(page)
  await expect(page).toHaveURL(`${baseURL}/favoritos`)
  await expect(page.getByRole('heading', { name: 'Mis favoritos', exact: true })).toBeVisible()
})
