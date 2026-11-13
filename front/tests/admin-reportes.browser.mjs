import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { chromium, expect } from '@playwright/test'
import { createServer } from 'vite'

const baseURL = 'http://127.0.0.1:15179'
const admin = { id: 5, nombre: 'Ana', apellido: 'Pérez', correo: 'ana@example.com', direccion: 'Quilmes', role: 'ADMINISTRADOR' }
const titulos = ['Usuarios con más compras', 'Destinos mejor puntuados', 'Agencias con más ventas', 'Destinos más vendidos']
let server
let browser

before(async () => {
  server = await createServer({
    root: fileURLToPath(new URL('..', import.meta.url)),
    server: { host: '127.0.0.1', port: 15179, strictPort: true },
    define: { 'import.meta.env.VITE_API_URL': JSON.stringify(baseURL) },
  })
  await server.listen()
  browser = await chromium.launch({ channel: 'chrome', headless: true })
})

after(async () => {
  await browser?.close()
  await server?.close()
})

async function abrirAdministracion(t, responderReporte) {
  const context = await browser.newContext({ baseURL })
  const page = await context.newPage()
  const solicitudes = []
  const errores = []
  page.on('pageerror', (error) => errores.push(error.message))
  await page.route((url) => url.pathname.startsWith('/api/'), async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    if (path === '/api/auth/refresh') return route.fulfill({ json: { accessToken: 'admin-token', expiresIn: 900, user: admin } })
    if (path === '/api/usuarios' || path === '/api/agencias') return route.fulfill({ json: [] })
    if (path === '/api/admin/reportes') {
      solicitudes.push({ method: request.method(), authorization: request.headers().authorization })
      return route.fulfill(responderReporte())
    }
    errores.push(`Solicitud inesperada: ${request.method()} ${path}`)
    return route.fulfill({ status: 500, json: {} })
  })
  t.after(async () => { await context.close(); assert.deepEqual(errores, []) })

  await page.goto('/admin')
  await expect(page.getByRole('button', { name: 'Nuevo usuario', exact: true })).toBeEnabled()
  return { page, solicitudes }
}

test('an administrator loads the four rankings on demand and can retry a failed request', async (t) => {
  const reportes = {
    usuariosConMasCompras: [
      { usuarioId: 11, nombre: 'Lucía', apellido: 'Pérez', totalCompras: 7 },
      { usuarioId: 12, nombre: 'Pablo', apellido: 'Sosa', totalCompras: 4 },
    ],
    destinosMejorRankeados: [{ destino: 'Salta', promedio: 8.5, totalValoraciones: 2 }],
    agenciasConMasVentas: [{ agenciaId: 3, nombre: 'Viajes del Sur', totalVentas: 9 }],
    destinosMasVendidos: [{ destino: 'Bariloche', totalVentas: 6 }],
  }
  let disponible = false
  const { page, solicitudes } = await abrirAdministracion(t, () => disponible
    ? { json: reportes }
    : { status: 503, json: { message: 'Servicio no disponible' } })

  assert.deepEqual(solicitudes, [])
  await page.getByRole('button', { name: 'Reportes', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Reportes', exact: true })).toBeVisible()
  await expect(page.getByRole('alert')).toBeVisible()
  const intentosFallidos = solicitudes.length
  assert.ok(intentosFallidos > 0)
  disponible = true
  await page.getByRole('button', { name: 'Reintentar', exact: true }).click()

  for (const titulo of titulos) {
    await expect(page.getByRole('heading', { name: titulo, exact: true })).toBeVisible()
  }
  await expect(page.getByRole('main').getByRole('listitem')).toHaveText([
    /Lucía Pérez.*7 compras/, /Pablo Sosa.*4 compras/, /Salta.*8,5\s*\/\s*10.*2 valoraciones/,
    /Viajes del Sur.*9 ventas/, /Bariloche.*6 ventas/,
  ])
  assert.ok(solicitudes.length > intentosFallidos)
  for (const solicitud of solicitudes) {
    assert.deepEqual(solicitud, { method: 'GET', authorization: 'Bearer admin-token' })
  }
})

test('an empty response keeps the four rankings visible with empty states', async (t) => {
  const { page } = await abrirAdministracion(t, () => ({ json: {
    usuariosConMasCompras: [], destinosMejorRankeados: [], agenciasConMasVentas: [], destinosMasVendidos: [],
  } }))
  await page.getByRole('button', { name: 'Reportes', exact: true }).click()

  for (const titulo of titulos) {
    await expect(page.getByRole('heading', { name: titulo, exact: true })).toBeVisible()
  }
  await expect(page.getByText('Todavía no hay datos para este ranking.', { exact: true })).toHaveCount(4)
})
