import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { chromium, expect } from '@playwright/test'
import { createServer } from 'vite'

const baseURL = 'http://127.0.0.1:15175'
const paquete = {
  id: 1, origen: 'Buenos Aires', destino: 'Bariloche', precio: 1500,
  fechaInicial: '2027-12-01', fechaFinal: '2027-12-10',
  hotel: { nombre: 'Hotel del Lago', pais: 'Argentina', foto: '/hotel-demo.svg' },
  agencia: { id: 2, nombre: 'Viajes del Sur' },
}
let server
let browser

before(async () => {
  server = await createServer({
    root: fileURLToPath(new URL('..', import.meta.url)),
    server: { host: '127.0.0.1', port: 15175, strictPort: true },
    define: { 'import.meta.env.VITE_API_URL': JSON.stringify(baseURL) },
  })
  await server.listen()
  browser = await chromium.launch({ channel: 'chrome', headless: true })
})

after(async () => {
  await browser?.close()
  await server?.close()
})

async function openPage(t, { role, favorite = false, photo = paquete.hotel.foto, detailResponse = { status: 200 }, detailGate, mobile = false } = {}) {
  const context = await browser.newContext({ baseURL, viewport: mobile ? { width: 390, height: 844 } : { width: 1280, height: 900 } })
  const page = await context.newPage()
  page.setDefaultTimeout(8000)
  const requests = []
  const errors = []
  const favorites = new Set(favorite ? [paquete.id] : [])
  const compras = []
  const detail = { ...paquete, hotel: { ...paquete.hotel, foto: photo } }
  page.on('pageerror', (error) => errors.push(error.message))
  await page.route('**/hotel-demo.svg', (route) => route.fulfill({ contentType: 'image/svg+xml', body: '<svg xmlns="http://www.w3.org/2000/svg" width="800" height="400"><rect width="800" height="400" fill="#0369a1"/></svg>' }))
  await page.route((url) => url.pathname.startsWith('/api/'), async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    const method = request.method()
    requests.push({ path, method, params: Object.fromEntries(url.searchParams), body: request.postDataJSON() })
    if (method === 'OPTIONS') return route.fulfill({ status: 204 })
    if (path === '/api/auth/refresh') return route.fulfill(role ? {
      json: { accessToken: 'test-token', expiresIn: 900, user: { id: 5, nombre: 'Ana', apellido: 'Pérez', role } },
    } : { status: 401, json: {} })
    if (path === '/api/paquetes') return route.fulfill({ json: [paquete] })
    if (path === '/api/paquetes/buscar') return route.fulfill({ json: {
      contenido: [paquete], pagina: Number(url.searchParams.get('pagina') ?? 0), tamanio: 12, totalElementos: 24, totalPaginas: 2,
    } })
    if (path.startsWith('/api/paquetes/')) {
      await detailGate
      const status = path === '/api/paquetes/1' ? detailResponse.status : 404
      return route.fulfill({ status, json: status === 200 ? detail : { message: 'Error de prueba' } })
    }
    if (path === '/api/agencias') return route.fulfill({ json: [paquete.agencia] })
    if (path === '/api/favoritos') return route.fulfill({ json: [...favorites].map(() => ({ paquete, agregadoEn: '2026-10-21T12:00:00Z' })) })
    if (path === '/api/favoritos/1') {
      if (method === 'PUT') favorites.add(1)
      else favorites.delete(1)
      return route.fulfill({ status: 204 })
    }
    if (path === '/api/compras') {
      if (method === 'GET') return route.fulfill({ json: compras })
      const body = request.postDataJSON()
      const compra = { id: compras.length + 1, operacionId: body.operacionId, precio: 1500, fecha: '2026-10-21T12:00:00Z', paquete }
      compras.push(compra)
      return route.fulfill({ status: 201, json: compra })
    }
    errors.push(`Unexpected request: ${method} ${path}`)
    return route.fulfill({ status: 500, json: {} })
  })
  t.after(async () => { await context.close(); assert.deepEqual(errors, []) })
  return { page, requests, compras }
}

test('a visitor opens the card image, reloads its URL and uses back/forward', async (t) => {
  const { page } = await openPage(t)
  await page.goto('/')
  await page.getByRole('article').click({ position: { x: 100, y: 50 } })
  await expect(page).toHaveURL(`${baseURL}/paquetes/1`)
  await expect(page.getByRole('heading', { name: 'Viaje a Bariloche' })).toBeVisible()
  for (const value of ['Buenos Aires', 'Hotel del Lago', 'Viajes del Sur', '1 de diciembre de 2027', '10 de diciembre de 2027']) {
    await expect(page.getByText(value, { exact: true })).toBeVisible()
  }
  await expect(page.getByRole('img', { name: 'Hotel Hotel del Lago' })).toHaveAttribute('src', '/hotel-demo.svg')
  await expect(page.getByRole('button', { name: 'Comprar', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: /Agregar a favoritos/ })).toHaveCount(0)
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Viaje a Bariloche' })).toBeVisible()
  await page.getByRole('button', { name: 'Volver al listado' }).click()
  await expect(page.getByRole('heading', { name: 'Todos los paquetes' })).toBeVisible()
  await page.goForward()
  await expect(page.getByRole('heading', { name: 'Viaje a Bariloche' })).toBeVisible()
})

test('a direct shared URL loads only the detail, supports mobile and falls back for a broken photo', async (t) => {
  const { page, requests } = await openPage(t, { photo: '/missing-photo.jpg', mobile: true })
  await page.goto('/paquetes/1')
  await expect(page.getByRole('heading', { name: 'Viaje a Bariloche' })).toBeVisible()
  await expect(page.getByRole('img', { name: /foto del hotel no disponible/ })).toBeVisible()
  assert.equal(requests.some(({ path }) => path === '/api/paquetes'), false)
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth), false)
  await page.getByRole('button', { name: 'Volver al listado' }).click()
  await expect(page).toHaveURL(`${baseURL}/`)
  await expect(page.getByRole('heading', { name: 'Todos los paquetes' })).toBeVisible()
})

test('results retain destination, ordering, agency, price and pagination after visiting the detail', async (t) => {
  const { page, requests } = await openPage(t)
  await page.goto('/')
  await page.getByRole('button', { name: 'Bariloche', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Resultados de búsqueda' })).toBeVisible()
  const filtros = page.getByRole('complementary', { name: 'Filtros' })
  await filtros.getByLabel('Agencia').selectOption('2')
  await page.getByLabel('Precio mínimo').fill('100')
  await page.getByRole('button', { name: 'Aplicar filtros' }).click()
  await page.getByLabel('Ordenar por').selectOption('PRECIO_DESC')
  await page.getByRole('button', { name: 'Siguiente', exact: true }).click()
  await expect(page.getByText('Página 2 de 2')).toBeVisible()
  const expected = requests.filter(({ path }) => path === '/api/paquetes/buscar').at(-1).params
  await page.getByRole('link', { name: 'Ver detalle del viaje a Bariloche' }).click()
  await page.getByRole('button', { name: 'Volver al listado' }).click()
  await expect(page.getByText('Página 2 de 2')).toBeVisible()
  await expect(filtros.getByLabel('Agencia')).toHaveValue('2')
  await expect(page.getByLabel('Precio mínimo')).toHaveValue('100')
  await expect(page.getByLabel('Ordenar por')).toHaveValue('PRECIO_DESC')
  assert.deepEqual(requests.filter(({ path }) => path === '/api/paquetes/buscar').at(-1).params, expected)
  await page.reload()
  await expect(page.getByText('Página 2 de 2')).toBeVisible()
})

test('buyers reuse favorites and purchases across home, detail and favorites without card clicks interfering', async (t) => {
  const { page, compras } = await openPage(t, { role: 'COMPRADOR' })
  await page.goto('/')
  await page.getByRole('button', { name: 'Agregar a favoritos: Bariloche' }).click()
  await expect(page.getByRole('button', { name: 'Quitar de favoritos: Bariloche' })).toBeEnabled()
  await expect(page).toHaveURL(`${baseURL}/`)
  await page.getByRole('button', { name: 'Comprar viaje a Bariloche' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await expect(page).toHaveURL(`${baseURL}/`)
  await page.getByRole('button', { name: 'Cancelar', exact: true }).click()
  await page.getByRole('button', { name: 'Mis favoritos', exact: true }).click()
  await page.getByRole('link', { name: 'Ver detalle del viaje a Bariloche' }).click()
  await expect(page.getByRole('button', { name: 'Quitar de favoritos: Bariloche' })).toHaveAttribute('aria-pressed', 'true')
  await page.getByRole('button', { name: 'Comprar', exact: true }).click()
  await page.getByRole('button', { name: 'Confirmar compra', exact: true }).click()
  await expect(page.getByRole('heading', { name: '¡Compra confirmada!' })).toBeVisible()
  await page.getByRole('button', { name: 'Seguir explorando' }).click()
  await expect(page.getByText('Comprado 1 vez', { exact: true })).toBeVisible()
  assert.equal(compras.length, 1)
  await page.getByRole('button', { name: 'Volver al listado' }).click()
  await expect(page.getByRole('heading', { name: 'Mis favoritos', exact: true })).toBeVisible()
  await page.getByRole('link', { name: 'Ver detalle del viaje a Bariloche' }).click()
  await page.getByRole('button', { name: 'Quitar de favoritos: Bariloche' }).click()
  await expect(page.getByRole('button', { name: 'Agregar a favoritos: Bariloche' })).toHaveAttribute('aria-pressed', 'false')
  await page.getByRole('button', { name: 'Volver al listado' }).click()
  await expect(page.getByText('Todavía no guardaste paquetes favoritos.')).toBeVisible()
})

test('missing or invalid IDs show not-found and a transient failure can be retried', async (t) => {
  const { page, requests } = await openPage(t)
  for (const id of ['999', 'abc', '0', '-1', '9007199254740992']) {
    await page.goto(`/paquetes/${id}`)
    await expect(page.getByRole('heading', { name: 'Paquete no encontrado' })).toBeVisible()
  }
  const detailRequests = requests.filter(({ path }) => path.startsWith('/api/paquetes/'))
  assert.ok(detailRequests.length > 0)
  assert.ok(detailRequests.every(({ path }) => path === '/api/paquetes/999'))
  const detailResponse = { status: 503 }
  const retry = await openPage(t, { detailResponse, photo: '' })
  await retry.page.goto('/paquetes/1')
  await expect(retry.page.getByRole('alert')).toContainText('No se pudo cargar el paquete')
  detailResponse.status = 200
  await retry.page.getByRole('button', { name: 'Reintentar', exact: true }).click()
  await expect(retry.page.getByRole('heading', { name: 'Viaje a Bariloche' })).toBeVisible()
  await expect(retry.page.getByRole('img', { name: /foto del hotel no disponible/ })).toBeVisible()
})

test('agents and administrators can read a shared URL without buyer actions', async (t) => {
  for (const role of ['AGENTE', 'ADMINISTRADOR']) {
    const { page } = await openPage(t, { role })
    await page.goto('/paquetes/1')
    await expect(page.getByRole('heading', { name: 'Viaje a Bariloche' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Comprar', exact: true })).toHaveCount(0)
    await expect(page.getByRole('button', { name: /favoritos/ })).toHaveCount(0)
  }
})

test('the detail announces loading while its request is pending', async (t) => {
  let complete
  const detailGate = new Promise((resolve) => { complete = resolve })
  t.after(() => complete())
  const { page } = await openPage(t, { detailGate })
  await page.goto('/paquetes/1')
  await expect(page.getByRole('status')).toHaveText('Cargando paquete...')
  complete()
  await expect(page.getByRole('heading', { name: 'Viaje a Bariloche' })).toBeVisible()
})
