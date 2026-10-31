import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { chromium, expect } from '@playwright/test'
import { createServer } from 'vite'

const baseURL = 'http://127.0.0.1:15177'
const paquete = {
  id: 1, origen: 'Buenos Aires', destino: 'Bariloche', precio: 1500, disponibilidad: 12,
  fechaInicial: '2027-12-01', fechaFinal: '2027-12-10',
  hotel: { nombre: 'Hotel del Lago', pais: 'Argentina', foto: '' }, agencia: { id: 2, nombre: 'Viajes del Sur' },
}
let server
let browser
before(async () => {
  server = await createServer({ root: fileURLToPath(new URL('..', import.meta.url)),
    server: { host: '127.0.0.1', port: 15177, strictPort: true }, define: { 'import.meta.env.VITE_API_URL': JSON.stringify(baseURL) } })
  await server.listen()
  browser = await chromium.launch({ channel: 'chrome', headless: true })
})
after(async () => { await browser?.close(); await server?.close() })

async function openPage(t, { role = 'COMPRADOR', mobile = false, failSave = false, failRead = false } = {}) {
  const context = await browser.newContext({ baseURL, viewport: mobile ? { width: 390, height: 844 } : { width: 1280, height: 900 } })
  const page = await context.newPage()
  page.setDefaultTimeout(8000)
  const errors = []
  const requests = []
  const ratings = new Map()
  const compras = [1, 2].map((id) => ({ id, precio: 1500, fecha: '2026-10-25', operacionId: `op-${id}`, paquete }))
  page.on('pageerror', (error) => errors.push(error.message))
  await page.route((url) => url.pathname.startsWith('/api/'), async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    const method = request.method()
    requests.push({ method, path })
    if (method === 'OPTIONS') return route.fulfill({ status: 204 })
    if (path === '/api/auth/refresh') return route.fulfill(role
      ? { json: { accessToken: 'buyer-token', expiresIn: 900, user: { id: 5, nombre: 'Ana', apellido: 'Privado', role } } }
      : { status: 401, json: {} })
    if (path === '/api/favoritos') return route.fulfill({ json: [] })
    if (path === '/api/compras' && method === 'GET') return route.fulfill({ json: compras })
    if (path === '/api/paquetes') return route.fulfill({ json: [paquete] })
    if (path === '/api/paquetes/1') return route.fulfill({ json: paquete })
    if (path === '/api/agencias') return route.fulfill({ json: [paquete.agencia] })
    if (path === '/api/valoraciones/mias') {
      if (failRead) return route.fulfill({ status: 503, json: { message: 'Error temporal' } })
      return route.fulfill({ json: [...ratings].map(([compraId, valoracion]) => ({ compraId, valoracion })) })
    }
    if (path === '/api/paquetes/1/valoraciones') {
      const valoraciones = [...ratings.values()]
      return route.fulfill({ json: { total: valoraciones.length, promedio: valoraciones.length ? valoraciones.reduce((sum, r) => sum + r.puntaje, 0) / valoraciones.length : null, valoraciones } })
    }
    const match = path.match(/^\/api\/compras\/(\d+)\/valoracion$/)
    if (match) {
      assert.equal(request.headers().authorization, 'Bearer buyer-token')
      const compraId = Number(match[1])
      if (method === 'DELETE') { ratings.delete(compraId); return route.fulfill({ status: 204 }) }
      if (failSave) { failSave = false; return route.fulfill({ status: 503, json: { message: 'No se pudo guardar' } }) }
      const datos = request.postDataJSON()
      const valoracion = { id: compraId, ...datos, comentario: datos.comentario ?? null, fecha: '2026-10-25T12:00:00Z' }
      ratings.set(compraId, valoracion)
      return route.fulfill({ json: { compraId, valoracion } })
    }
    errors.push(`Unexpected request: ${method} ${path}`)
    return route.fulfill({ status: 500, json: {} })
  })
  t.after(async () => { await context.close(); assert.deepEqual(errors, []) })
  return { page, ratings, requests, compras, permitirLectura: () => { failRead = false } }
}

async function abrirCompras(page) {
  await page.goto('/compras')
  await page.getByText('Ver detalle de las 2 compras').click()
  await expect(page.getByRole('button', { name: 'Valorar compra' }).first()).toBeEnabled()
}

test('a buyer rates each purchase separately, edits, deletes and sees the public average on mobile', async (t) => {
  const { page, requests } = await openPage(t, { mobile: true })
  await abrirCompras(page)
  await page.getByRole('button', { name: 'Valorar compra' }).first().click()
  let dialog = page.getByRole('dialog')
  await dialog.getByLabel('Puntaje (0 a 10)').fill('0')
  await dialog.getByRole('button', { name: 'Guardar valoración' }).click()
  await expect(dialog).toHaveCount(0)
  await expect(page.getByText('Tu puntaje: 0/10')).toBeVisible()
  await page.getByRole('button', { name: 'Valorar compra', exact: true }).click()
  dialog = page.getByRole('dialog')
  await dialog.getByLabel('Puntaje (0 a 10)').fill('10')
  await dialog.getByLabel('Comentario (opcional)').fill('Excelente viaje')
  await dialog.getByRole('button', { name: 'Guardar valoración' }).click()
  await expect(dialog).toHaveCount(0)
  await page.reload()
  await page.getByText('Ver detalle de las 2 compras').click()
  await expect(page.getByText('Tu puntaje: 10/10')).toBeVisible()
  await page.getByRole('button', { name: 'Editar valoración' }).first().click()
  dialog = page.getByRole('dialog')
  await dialog.getByLabel('Puntaje (0 a 10)').fill('6')
  await dialog.getByRole('button', { name: 'Guardar valoración' }).click()
  await expect(page.getByText('Tu puntaje: 6/10')).toBeVisible()
  await page.goto('/paquetes/1')
  const publicas = page.getByRole('region', { name: 'Valoraciones del paquete' })
  await expect(publicas.getByText('8/10', { exact: true })).toBeVisible()
  await expect(publicas.getByText('Excelente viaje')).toBeVisible()
  await expect(publicas).not.toContainText('Ana')
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth), false)
  await page.goto('/compras')
  await page.getByText('Ver detalle de las 2 compras').click()
  await page.getByRole('button', { name: 'Editar valoración' }).first().click()
  await page.getByRole('button', { name: 'Eliminar valoración' }).click()
  await expect(page.getByText('Valoración eliminada. Tu compra se conserva.')).toBeVisible()
  assert.equal(requests.filter(({ method, path }) => method === 'POST' && path === '/api/compras').length, 0)
})

test('rating failure preserves the form and purchases, and retries only the rating', async (t) => {
  const { page, compras, requests } = await openPage(t, { failSave: true })
  await abrirCompras(page)
  await page.getByRole('button', { name: 'Valorar compra' }).first().click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('Puntaje (0 a 10)').fill('9')
  await dialog.getByLabel('Comentario (opcional)').fill('Mi comentario')
  await dialog.getByRole('button', { name: 'Guardar valoración' }).click()
  await expect(dialog.getByRole('alert')).toContainText('No se pudo guardar')
  await expect(dialog.getByLabel('Comentario (opcional)')).toHaveValue('Mi comentario')
  await dialog.getByRole('button', { name: 'Guardar valoración' }).click()
  await expect(dialog).toHaveCount(0)
  assert.equal(compras.length, 2)
  assert.equal(requests.filter(({ method }) => method === 'PUT').length, 2)
  assert.equal(requests.some(({ method, path }) => method === 'POST' && path === '/api/compras'), false)
})

test('unknown saved ratings disable editing until their list is successfully retried', async (t) => {
  const { page, permitirLectura } = await openPage(t, { failRead: true })
  await page.goto('/compras')
  await page.getByText('Ver detalle de las 2 compras').click()
  await expect(page.getByRole('alert')).toContainText('No se pudieron cargar tus valoraciones')
  await expect(page.getByRole('button', { name: 'Valorar compra' }).first()).toBeDisabled()
  permitirLectura()
  await page.getByRole('button', { name: 'Reintentar valoraciones' }).click()
  await expect(page.getByRole('button', { name: 'Valorar compra' }).first()).toBeEnabled()
})

test('visitors and other roles can read anonymous ratings without editing controls', async (t) => {
  for (const role of [null, 'ADMINISTRADOR', 'AGENTE']) {
    const { page, ratings } = await openPage(t, { role })
    ratings.set(1, { id: 1, puntaje: 0, comentario: 'Opinion publica', fecha: '2026-10-25T12:00:00Z' })
    await page.goto('/paquetes/1')
    const publicas = page.getByRole('region', { name: 'Valoraciones del paquete' })
    await expect(publicas.getByText('Opinion publica')).toBeVisible()
    await expect(publicas.getByText('0/10', { exact: true })).toHaveCount(2)
    await expect(page.getByRole('button', { name: /Valorar compra|Editar valoración/ })).toHaveCount(0)
  }
})
