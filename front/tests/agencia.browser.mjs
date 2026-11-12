import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { chromium, expect } from '@playwright/test'
import { createServer } from 'vite'

const baseURL = 'http://127.0.0.1:15177'
const hotel = { id: 3, nombre: 'Hotel del Lago', pais: 'Argentina' }
const agente = { id: 5, nombre: 'Ana', apellido: 'Pérez', correo: 'ana@example.com', direccion: 'Quilmes', role: 'AGENTE' }
const paquete = {
  id: 1, origen: 'Buenos Aires', destino: 'Bariloche', precio: 1500, disponibilidad: 12, vueloIdaId: 10, vueloVueltaId: 11,
  fechaInicial: '2027-12-01', fechaFinal: '2027-12-10', hotel, agencia: { id: 2, nombre: 'Viajes del Sur' },
}
const vuelo = (id, origen, destino, fecha, disponibilidad = 8) => ({ id, origen, destino, fecha, hora: '08:30:00', aerolinea: 'Aerolíneas', disponibilidad })
const vuelos = [
  vuelo(20, 'Rosario', 'Salta', '2027-11-10'), vuelo(21, 'Salta', 'Rosario', '2027-11-20'),
  vuelo(22, 'Salta', 'Rosario', '2027-11-05'), vuelo(23, 'Salta', 'Mendoza', '2027-11-21'), vuelo(24, 'Salta', 'Rosario', '2027-11-25', 0),
]
const venta = (id, cliente, precio, fecha) => ({
  id, operacionId: `op-${id}`, precio, fecha, paqueteId: 1, origen: 'Buenos Aires', destino: 'Bariloche', hotel: hotel.nombre, cliente,
})
const clienteLuis = { id: 7, nombre: 'Luis', apellido: 'Gómez', correo: 'luis@example.com' }
let server
let browser

before(async () => {
  server = await createServer({
    root: fileURLToPath(new URL('..', import.meta.url)),
    server: { host: '127.0.0.1', port: 15177, strictPort: true },
    define: { 'import.meta.env.VITE_API_URL': JSON.stringify(baseURL) },
  })
  await server.listen()
  browser = await chromium.launch({ channel: 'chrome', headless: true })
})

after(async () => {
  await browser?.close()
  await server?.close()
})

async function openPage(t, { signedIn }) {
  const context = await browser.newContext({ baseURL })
  const page = await context.newPage()
  page.setDefaultTimeout(8000)
  const errors = []
  const bodies = []
  const paquetes = [paquete]
  page.on('pageerror', (error) => errors.push(error.message))
  await page.route((url) => url.pathname.startsWith('/api/'), async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    const method = request.method()
    if (method === 'OPTIONS') return route.fulfill({ status: 204 })
    if (path === '/api/auth/refresh') return route.fulfill(signedIn
      ? { json: { accessToken: 'test-token', expiresIn: 900, user: agente } } : { status: 401, json: {} })
    if (path === '/api/auth/register-agencia') {
      bodies.push(request.postDataJSON())
      return route.fulfill({ status: 201, json: agente })
    }
    if (path === '/api/usuarios/me') return route.fulfill({ json: { ...agente, rol: 'AGENTE', agenciaId: 2 } })
    if (path === '/api/vuelos') return route.fulfill({ json: vuelos })
    if (path === '/api/hoteles') return route.fulfill({ json: [hotel] })
    if (path === '/api/ventas') return route.fulfill({ json: [venta(2, clienteLuis, 1500, '2027-02-01'), venta(1, clienteLuis, 900, '2027-01-01')] })
    if (path === '/api/paquetes/buscar') return route.fulfill({
      json: { contenido: paquetes, pagina: 0, tamanio: 12, totalElementos: paquetes.length, totalPaginas: 1 },
    })
    if (path === '/api/paquetes' && method === 'POST') {
      const body = request.postDataJSON()
      bodies.push(body)
      paquetes.unshift({ ...paquete, ...body, id: 2, hotel, agencia: paquete.agencia })
      return route.fulfill({ status: 201, json: paquetes[0] })
    }
    if (path === '/api/paquetes/1' && method === 'DELETE') {
      paquetes.splice(0, paquetes.length)
      return route.fulfill({ status: 204 })
    }
    if (path === '/api/paquetes') return route.fulfill({ json: paquetes })
    if (path === '/api/agencias') return route.fulfill({ json: [paquete.agencia] })
    errors.push(`Unexpected request: ${method} ${path}`)
    return route.fulfill({ status: 500, json: {} })
  })
  t.after(async () => { await context.close(); assert.deepEqual(errors, []) })
  return { page, bodies }
}

test('an agent manages its packages from Mi agencia', async (t) => {
  const { page, bodies } = await openPage(t, { signedIn: true })
  await page.goto('/')
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Mi agencia', exact: true }).click()
  await expect(page).toHaveURL(`${baseURL}/agencia`)
  await expect(page.getByRole('row', { name: /Buenos Aires → Bariloche/ })).toBeVisible()

  await page.getByRole('button', { name: 'Nuevo paquete' }).click()
  const dialog = page.getByRole('dialog', { name: 'Nuevo paquete' })
  const elegir = async (nombre, opcion) => {
    await dialog.getByRole('combobox', { name: nombre }).click()
    await dialog.getByRole('option', { name: opcion }).click()
  }
  const dia = (numero) => dialog.getByRole('button', { name: new RegExp(`^${numero} de noviembre de 2027`) })
  // Flights wait for the dates.
  await expect(dialog.getByRole('combobox', { name: /^Vuelo de ida/ })).toBeDisabled()
  await dialog.getByRole('button', { name: /^Fechas del viaje/ }).click()
  // The calendar opens at the first month with flights and marks the days that have them.
  await expect(dialog.getByText('noviembre de 2027')).toBeVisible()
  await expect(dia(5)).toHaveAccessibleName(/hay vuelos/)
  await expect(dia(6)).not.toHaveAccessibleName(/hay vuelos/)
  await dia(10).click()
  await expect(dialog.getByRole('combobox', { name: /^Vuelo de ida/ })).toBeDisabled()
  // A day before the start restarts the range instead of inverting it.
  await dia(8).click()
  await dia(10).click()
  await expect(dialog.getByRole('button', { name: /^Fechas del viaje/ })).toContainText('8 de noviembre de 2027 – 10 de noviembre de 2027')
  await dialog.getByRole('button', { name: /^Fechas del viaje/ }).click()
  await dia(10).click()
  await dia(20).click()
  await expect(dialog.getByRole('button', { name: /^Fechas del viaje/ })).toContainText('10 de noviembre de 2027 – 20 de noviembre de 2027')

  // Only flights inside the dates and with seats (the others are before, after or full).
  const alto = () => dialog.evaluate((el) => el.scrollHeight)
  const altoCerrado = await alto()
  await dialog.getByRole('combobox', { name: /^Vuelo de ida/ }).click()
  await expect(dialog.getByRole('option')).toHaveCount(2)
  // The list floats over the form: the dialog neither grows nor starts scrolling, and the list stays on screen.
  assert.equal(await alto(), altoCerrado)
  const lista = await dialog.getByRole('listbox').boundingBox()
  const ventana = page.viewportSize()
  assert.ok(lista.y >= 0 && lista.y + lista.height <= ventana.height, 'the list fits in the viewport')
  await expect(dialog.getByRole('option').first()).toContainText('8 lugares')
  await dialog.getByRole('option', { name: /Rosario → Salta/ }).click()
  await expect(dialog.getByText('Viaje: Rosario → Salta')).toBeVisible()
  // Reverse route, on or after the outbound date, inside the dates, with seats.
  await dialog.getByRole('combobox', { name: /^Vuelo de vuelta/ }).click()
  await expect(dialog.getByRole('option')).toHaveCount(1)
  await page.keyboard.press('ArrowDown')
  await page.keyboard.press('Enter')
  await expect(dialog.getByRole('combobox', { name: /^Vuelo de vuelta/ })).toContainText('Salta → Rosario')
  await expect(dialog.getByRole('button', { name: 'Guardar paquete' })).toBeDisabled()
  await dialog.getByLabel('Precio (USD)').fill('900')
  await expect(dialog.getByRole('button', { name: 'Guardar paquete' })).toBeEnabled()

  // Changing the dates drops flights that no longer fit.
  await dialog.getByRole('button', { name: /^Fechas del viaje/ }).click()
  await dia(12).click()
  await dia(20).click()
  await expect(dialog.getByRole('combobox', { name: /^Vuelo de ida/ })).toContainText('Elegí el vuelo de ida')
  await expect(dialog.getByRole('button', { name: 'Guardar paquete' })).toBeDisabled()
  await dialog.getByRole('button', { name: /^Fechas del viaje/ }).click()
  await dia(10).click()
  await dia(20).click()
  await elegir(/^Vuelo de ida/ , /Rosario → Salta/)
  await elegir(/^Vuelo de vuelta/ , /Salta → Rosario/)
  await dialog.getByRole('combobox', { name: 'Hotel' }).click()
  await dialog.getByRole('option', { name: /Hotel del Lago/ }).click()
  await dialog.getByRole('button', { name: 'Guardar paquete' }).click()

  assert.deepEqual(bodies, [{
    precio: 900, vueloIdaId: 20, vueloVueltaId: 21, hotelId: 3, agenciaId: 2, destino: 'Salta', origen: 'Rosario',
    fechaInicial: '2027-11-10', fechaFinal: '2027-11-20',
  }])
  await expect(page.getByRole('status')).toHaveText('Paquete creado.')
  await expect(page.getByRole('row', { name: /Rosario → Salta/ })).toBeVisible()
  await expect(page.getByRole('row', { name: /Rosario → Salta/ }).getByRole('button', { name: 'Editar paquete a Salta' })).toBeEnabled()
})

test('the actions header explains with a tooltip why packages with purchases cannot be edited or deleted', async (t) => {
  const { page } = await openPage(t, { signedIn: true })
  await page.goto('/agencia')
  const fila = page.getByRole('row', { name: /Buenos Aires → Bariloche/ })
  const editar = fila.getByRole('button', { name: 'Editar paquete a Bariloche' })
  const eliminar = fila.getByRole('button', { name: 'Eliminar paquete a Bariloche' })
  const info = page.getByRole('button', { name: 'Más información sobre las acciones' })
  const regla = 'Los paquetes con compras registradas no se pueden editar ni eliminar'

  await expect(editar).toBeDisabled()
  await expect(eliminar).toBeDisabled()
  await expect(editar).toHaveAccessibleName(/no disponible: ya tiene compras/)
  await expect(page.getByRole('tooltip')).toHaveCount(0)
  await info.hover()
  await expect(page.getByRole('tooltip')).toContainText(regla)
  // The whole message fits in its box and on screen, wherever the header sits.
  const entra = async () => {
    const caja = await page.getByRole('tooltip').boundingBox()
    const { ancho, alto } = await page.evaluate(() => ({ ancho: innerWidth, alto: innerHeight }))
    const desborda = await page.getByRole('tooltip').evaluate((el) => el.scrollWidth > el.clientWidth || el.scrollHeight > el.clientHeight)
    assert.equal(desborda, false, 'the text overflows the tooltip box')
    assert.ok(caja.x >= 0 && caja.y >= 0 && caja.x + caja.width <= ancho && caja.y + caja.height <= alto, 'the tooltip is inside the viewport')
  }
  await entra()
  for (const tamano of [{ width: 360, height: 640 }, { width: 1280, height: 300 }]) {
    await page.setViewportSize(tamano)
    await info.hover()
    await entra()
  }
  await page.setViewportSize({ width: 1280, height: 720 })
  await page.mouse.move(0, 0)
  await expect(page.getByRole('tooltip')).toBeHidden()
  // Keyboard users get it too, and the icon describes itself with it.
  await info.focus()
  await expect(page.getByRole('tooltip')).toBeVisible()
  await expect(info).toHaveAccessibleDescription(new RegExp(regla))
  // The inert actions open nothing.
  await editar.dispatchEvent('click')
  await eliminar.dispatchEvent('click')
  await expect(page.getByRole('dialog')).toHaveCount(0)
})

test('an agent consults its sales and clients', async (t) => {
  const { page } = await openPage(t, { signedIn: true })
  await page.goto('/agencia')
  await page.getByRole('button', { name: 'Ventas y clientes' }).click()
  await expect(page.getByText('2 ventas · 1 cliente · Total')).toBeVisible()
  await expect(page.getByRole('table', { name: 'Clientes' }).getByRole('row', { name: /Luis Gómez luis@example.com 2/ })).toBeVisible()
})

test('a visitor registers an agency and is sent to login', async (t) => {
  const { page, bodies } = await openPage(t, { signedIn: false })
  await page.goto('/registro')
  await page.getByRole('button', { name: 'Registrar una agencia' }).click()
  await expect(page).toHaveURL(`${baseURL}/registro-agencia`)
  await page.getByLabel('Nombre de la agencia').fill('Viajes del Sur')
  await page.getByLabel('Correo de la agencia').fill('sur@example.com')
  await page.getByLabel('Nombre', { exact: true }).fill('Ana')
  await page.getByLabel('Apellido', { exact: true }).fill('Pérez')
  await page.getByLabel('Dirección').fill('Quilmes')
  await page.getByLabel('Correo electrónico').fill('ana@example.com')
  await page.getByLabel('Contraseña').fill('password123')
  await page.getByRole('button', { name: 'Registrar mi agencia' }).click()

  await expect(page).toHaveURL(`${baseURL}/login`)
  assert.deepEqual(bodies, [{
    usuario: { nombre: 'Ana', apellido: 'Pérez', correo: 'ana@example.com', direccion: 'Quilmes', password: 'password123' },
    agencia: { nombre: 'Viajes del Sur', email: 'sur@example.com' },
  }])
})

test('a visitor asking for the agency panel is sent to login', async (t) => {
  const { page } = await openPage(t, { signedIn: false })
  await page.goto('/agencia')
  await expect(page).toHaveURL(`${baseURL}/login`)
})
