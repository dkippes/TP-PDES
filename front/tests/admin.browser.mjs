import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { chromium, expect } from '@playwright/test'
import { createServer } from 'vite'

const baseURL = 'http://127.0.0.1:15178'
const agencias = [{ id: 1, nombre: 'Viajes del Sur' }, { id: 2, nombre: 'Rumbo Andino' }]
const cuentas = [
  { id: 1, nombre: 'Admin', rol: 'ADMINISTRADOR', agenciaId: null },
  { id: 2, nombre: 'Administradora', rol: 'ADMINISTRADOR', agenciaId: null },
  { id: 3, nombre: 'Agente', rol: 'AGENTE', agenciaId: 1 },
  { id: 4, nombre: 'Compradora', rol: 'COMPRADOR', agenciaId: null },
].map((cuenta) => ({ ...cuenta, apellido: 'Prueba', correo: `cuenta${cuenta.id}@example.com`, direccion: 'Quilmes' }))
let server
let browser

before(async () => {
  server = await createServer({
    root: fileURLToPath(new URL('..', import.meta.url)),
    server: { host: '127.0.0.1', port: 15178, strictPort: true },
    define: { 'import.meta.env.VITE_API_URL': JSON.stringify(baseURL) },
  })
  await server.listen()
  browser = await chromium.launch({ channel: 'chrome', headless: true })
})

after(async () => { await browser?.close(); await server?.close() })

async function abrirAdministracion(t) {
  const context = await browser.newContext({ baseURL })
  const page = await context.newPage()
  page.setDefaultTimeout(8000)
  const usuarios = cuentas.map((cuenta) => ({ ...cuenta }))
  const guardados = []
  const errors = []
  page.on('pageerror', (error) => errors.push(error.message))
  await page.route((url) => url.pathname.startsWith('/api/'), async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    const method = request.method()
    if (method === 'OPTIONS') return route.fulfill({ status: 204 })
    if (path === '/api/auth/refresh') return route.fulfill({ json: {
      accessToken: 'admin-token', expiresIn: 900,
      user: { ...cuentas[0], role: 'ADMINISTRADOR' },
    } })
    if (path === '/api/agencias') return route.fulfill({ json: agencias })
    if (path === '/api/paquetes') return route.fulfill({ json: [] })
    if (path === '/api/usuarios' && method === 'GET') return route.fulfill({ json: usuarios })
    if (path === '/api/usuarios' && method === 'POST') {
      const datos = request.postDataJSON()
      guardados.push(datos)
      const { password: _password, ...cuenta } = datos
      const creado = { ...cuenta, id: usuarios.length + 1 }
      usuarios.push(creado)
      return route.fulfill({ status: 201, json: creado })
    }
    const match = path.match(/^\/api\/usuarios\/(\d+)$/)
    if (match && method === 'PUT') {
      const datos = request.postDataJSON()
      const usuario = usuarios.find((cuenta) => cuenta.id === Number(match[1]))
      guardados.push(datos)
      assert.equal(datos.rol, usuario.rol)
      Object.assign(usuario, datos)
      return route.fulfill({ json: usuario })
    }
    errors.push(`Unexpected request: ${method} ${path}`)
    return route.fulfill({ status: 500, json: {} })
  })
  t.after(async () => { await context.close(); assert.deepEqual(errors, []) })
  await page.goto('/admin')
  await expect(page.getByRole('button', { name: 'Nuevo usuario', exact: true })).toBeEnabled()
  return { page, guardados }
}

test('editing any account keeps its role fixed while allowing personal data and agent agency changes', async (t) => {
  const { page, guardados } = await abrirAdministracion(t)
  for (const cuenta of cuentas) {
    await page.getByRole('button', { name: `Editar usuario ${cuenta.nombre} ${cuenta.apellido}`, exact: true }).click()
    const dialog = page.getByRole('dialog', { name: 'Editar usuario' })
    await expect(dialog.getByLabel('Rol', { exact: true })).toBeDisabled()
    await expect(dialog.getByLabel('Rol', { exact: true })).toHaveValue(cuenta.rol)
    await expect(dialog.getByText('El rol se define al crear la cuenta y no se puede modificar.')).toBeVisible()
    await dialog.getByLabel('Nombre', { exact: true }).fill(`${cuenta.nombre} Editada`)
    if (cuenta.rol === 'AGENTE') {
      await expect(dialog.getByLabel('Agencia', { exact: true })).toBeEnabled()
      await dialog.getByLabel('Agencia', { exact: true }).selectOption('2')
    }
    await dialog.getByRole('button', { name: 'Guardar usuario' }).click()
    await expect(dialog).toHaveCount(0)
    const enviado = guardados.at(-1)
    assert.equal(enviado.rol, cuenta.rol)
    assert.equal(enviado.nombre, `${cuenta.nombre} Editada`)
    assert.equal(enviado.agenciaId, cuenta.rol === 'AGENTE' ? 2 : null)
    assert.equal('password' in enviado, false)
    await expect(page.getByRole('button', { name: `Editar usuario ${enviado.nombre} ${cuenta.apellido}`, exact: true })).toBeVisible()
  }
})

test('creating accounts still allows choosing each role and requires an agency for agents', async (t) => {
  const { page, guardados } = await abrirAdministracion(t)
  for (const rol of ['COMPRADOR', 'AGENTE', 'ADMINISTRADOR']) {
    await page.getByRole('button', { name: 'Nuevo usuario', exact: true }).click()
    const dialog = page.getByRole('dialog', { name: 'Nuevo usuario' })
    await expect(dialog.getByLabel('Rol', { exact: true })).toBeEnabled()
    await dialog.getByLabel('Rol', { exact: true }).selectOption(rol)
    await dialog.getByLabel('Nombre', { exact: true }).fill('Nueva')
    await dialog.getByLabel('Apellido', { exact: true }).fill(rol)
    await dialog.getByLabel('Correo electrónico').fill(`nuevo.${rol.toLowerCase()}@example.com`)
    await dialog.getByLabel('Dirección').fill('Quilmes')
    await dialog.getByLabel('Contraseña', { exact: true }).fill('12345678')
    if (rol === 'AGENTE') {
      await dialog.getByRole('button', { name: 'Guardar usuario' }).click()
      assert.equal(guardados.length, 1)
      await expect(dialog.getByLabel('Agencia', { exact: true })).toHaveAttribute('required', '')
      await dialog.getByLabel('Agencia', { exact: true }).selectOption('1')
    }
    await dialog.getByRole('button', { name: 'Guardar usuario' }).click()
    await expect(dialog).toHaveCount(0)
    assert.equal(guardados.at(-1).rol, rol)
    assert.equal(guardados.at(-1).agenciaId, rol === 'AGENTE' ? 1 : null)
    assert.equal(guardados.at(-1).password, '12345678')
  }
  assert.equal(guardados.length, 3)
})
