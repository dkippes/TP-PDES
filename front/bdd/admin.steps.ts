import { expect } from '@playwright/test'
import { createBdd } from 'playwright-bdd'
import { test } from './auth.steps.ts'

const { Given, When, Then } = createBdd(test)
const dialog = (page: import('@playwright/test').Page) => page.getByRole('dialog')

Given('que ingreso a la Home con rol {string}', async ({ page }, rol: string) => {
  await page.route('**/api/paquetes', (route) => route.fulfill({ json: [] }))
  let usuarios = [
    { id: 1, nombre: 'Admin', apellido: 'Demo', correo: 'admin@example.com', direccion: 'Quilmes', rol: 'ADMINISTRADOR', agenciaId: null as number | null },
    { id: 2, nombre: 'Ana', apellido: 'Demo', correo: 'ana@example.com', direccion: 'Quilmes', rol: 'COMPRADOR', agenciaId: null as number | null },
    { id: 3, nombre: 'Agente', apellido: 'Demo', correo: 'agente@example.com', direccion: 'Quilmes', rol: 'AGENTE', agenciaId: 1 as number | null },
  ]
  let agencias = [
    { id: 1, nombre: 'Viajes del Sur', email: 'sur@example.com' },
    { id: 2, nombre: 'Horizonte Turismo', email: 'horizonte@example.com' },
  ]
  await page.route('**/api/auth/refresh', (route) => route.fulfill({
    json: { accessToken: 'admin-test-token', tokenType: 'Bearer', expiresIn: 900, user: { ...usuarios[0], role: rol } },
  }))
  await page.route(/\/api\/(usuarios|agencias)(?:\/\d+)?$/, async (route) => {
    expect(rol).toBe('ADMINISTRADOR')
    expect(route.request().headers().authorization).toBe('Bearer admin-test-token')
    const path = new URL(route.request().url()).pathname
    const isUser = path.startsWith('/api/usuarios')
    const id = Number(path.split('/')[3])
    const method = route.request().method()
    if (method === 'GET') return route.fulfill({ json: isUser ? usuarios : agencias })
    if (method === 'DELETE') {
      if (!isUser && id === 1) return route.fulfill({ status: 409, json: { message: 'La agencia tiene referencias que impiden eliminarla' } })
      if (isUser) usuarios = usuarios.filter((usuario) => usuario.id !== id)
      else agencias = agencias.filter((agencia) => agencia.id !== id)
      return route.fulfill({ status: 204 })
    }
    const body = route.request().postDataJSON()
    if (isUser) {
      if (method === 'POST') {
        expect(body.rol).toBe('AGENTE')
        expect(body.agenciaId).toBe(1)
        expect(body.password).toBe('12345678')
      } else {
        expect(body.rol).toBe('COMPRADOR')
        expect(body.agenciaId).toBeNull()
        expect(body).not.toHaveProperty('password')
      }
      const saved = { ...body, id: method === 'POST' ? 10 : id }
      usuarios = [...usuarios.filter((usuario) => usuario.id !== saved.id), saved]
      return route.fulfill({ status: method === 'POST' ? 201 : 200, json: saved })
    }
    expect(body).toHaveProperty('email')
    expect(body).not.toHaveProperty('correo')
    const saved = { ...body, id: method === 'POST' ? 10 : id }
    agencias = [...agencias.filter((agencia) => agencia.id !== saved.id), saved]
    return route.fulfill({ status: method === 'POST' ? 201 : 200, json: saved })
  })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Todos los paquetes' })).toBeVisible()
  if (rol === 'ADMINISTRADOR') {
    await page.getByRole('button', { name: 'Administración', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Administración', exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Nuevo usuario', exact: true })).toBeEnabled()
  }
})

When('creo un usuario agente con una agencia', async ({ page }) => {
  await page.getByRole('button', { name: 'Nuevo usuario', exact: true }).click()
  await page.getByLabel('Nombre', { exact: true }).fill('Lucia')
  await page.getByLabel('Apellido', { exact: true }).fill('Prueba')
  await page.getByLabel('Correo electrónico', { exact: true }).fill('lucia@example.com')
  await page.getByLabel('Dirección', { exact: true }).fill('Quilmes')
  await page.getByLabel('Contraseña', { exact: true }).fill('12345678')
  await page.getByLabel('Rol', { exact: true }).selectOption('AGENTE')
  await page.getByRole('button', { name: 'Guardar usuario', exact: true }).click()
  await expect(page.getByLabel('Agencia', { exact: true })).toBeFocused()
  await page.getByLabel('Agencia', { exact: true }).selectOption('1')
  await page.getByRole('button', { name: 'Guardar usuario', exact: true }).click()
})
Then('veo el usuario agente creado', async ({ page }) => {
  const row = page.getByRole('row').filter({ hasText: 'lucia@example.com' })
  await expect(row).toContainText('Agente')
  await expect(row).toContainText('Viajes del Sur')
})
When('cambio el usuario creado a comprador sin cambiar su contraseña', async ({ page }) => {
  await page.getByRole('button', { name: 'Editar usuario Lucia Prueba', exact: true }).click()
  await page.getByLabel('Nombre', { exact: true }).fill('Lucia editada')
  await page.getByLabel('Rol', { exact: true }).selectOption('COMPRADOR')
  await expect(page.getByLabel('Agencia', { exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: 'Guardar usuario', exact: true }).click()
})
Then('veo el usuario actualizado como comprador', async ({ page }) => {
  const row = page.getByRole('row').filter({ hasText: 'lucia@example.com' })
  await expect(row).toContainText('Lucia editada')
  await expect(row).toContainText('Comprador')
  await expect(row).not.toContainText('Viajes del Sur')
})
When('cancelo la eliminación del usuario creado', async ({ page }) => {
  await page.getByRole('button', { name: 'Eliminar usuario Lucia editada Prueba', exact: true }).click()
  await dialog(page).getByRole('button', { name: 'Cancelar', exact: true }).click()
})
Then('el usuario creado sigue registrado', async ({ page }) => {
  await expect(page.getByRole('row').filter({ hasText: 'lucia@example.com' })).toBeVisible()
})
When('confirmo la eliminación del usuario creado', async ({ page }) => {
  await page.getByRole('button', { name: 'Eliminar usuario Lucia editada Prueba', exact: true }).click()
  await dialog(page).getByRole('button', { name: 'Confirmar eliminación', exact: true }).click()
})
Then('el usuario creado deja de estar registrado', async ({ page }) => {
  await expect(page.getByRole('status')).toHaveText('Usuario eliminado.')
  await expect(page.getByRole('row').filter({ hasText: 'lucia@example.com' })).toHaveCount(0)
})

When('creo una agencia desde la Home', async ({ page }) => {
  await page.getByRole('button', { name: 'Agencias', exact: true }).click()
  await page.getByRole('button', { name: 'Nueva agencia', exact: true }).click()
  await page.getByLabel('Nombre de la agencia').fill('Ruta Prueba')
  await page.getByLabel('Correo de la agencia').fill('ruta@example.com')
  await page.getByRole('button', { name: 'Guardar agencia', exact: true }).click()
})
Then('veo la agencia creada', async ({ page }) => {
  await expect(page.getByRole('row').filter({ hasText: 'ruta@example.com' })).toContainText('Ruta Prueba')
})
When('edito el nombre de la agencia creada', async ({ page }) => {
  await page.getByRole('button', { name: 'Editar agencia Ruta Prueba', exact: true }).click()
  await page.getByLabel('Nombre de la agencia').fill('Ruta editada')
  await page.getByRole('button', { name: 'Guardar agencia', exact: true }).click()
})
Then('veo la agencia actualizada', async ({ page }) => {
  await expect(page.getByRole('row').filter({ hasText: 'ruta@example.com' })).toContainText('Ruta editada')
})
When('elimino la agencia creada', async ({ page }) => {
  await page.getByLabel('Buscar agencias').fill('ruta@example.com')
  await page.getByRole('button', { name: 'Eliminar agencia Ruta editada', exact: true }).click()
  await dialog(page).getByRole('button', { name: 'Confirmar eliminación', exact: true }).click()
})
Then('la agencia creada deja de estar registrada', async ({ page }) => {
  await expect(page.getByRole('status')).toHaveText('Agencia eliminada.')
  await expect(page.getByText('No hay agencias que coincidan con la búsqueda.')).toBeVisible()
})
When('intento eliminar una agencia con referencias', async ({ page }) => {
  await page.getByRole('button', { name: 'Agencias', exact: true }).click()
  await page.getByRole('button', { name: 'Eliminar agencia Viajes del Sur', exact: true }).click()
  await dialog(page).getByRole('button', { name: 'Confirmar eliminación', exact: true }).click()
})
Then('veo el rechazo del backend y la agencia sigue registrada', async ({ page }) => {
  await expect(dialog(page).getByRole('alert')).toHaveText('La agencia tiene referencias que impiden eliminarla')
  await dialog(page).getByRole('button', { name: 'Cancelar', exact: true }).click()
  await expect(page.getByRole('row').filter({ hasText: 'sur@example.com' })).toBeVisible()
})
When('filtro los usuarios por agente y consulto su detalle', async ({ page }) => {
  await page.getByLabel('Filtrar por rol').selectOption('AGENTE')
  await expect(page.getByRole('row')).toHaveCount(2)
  await page.getByLabel('Buscar usuarios').fill('nadie')
  await expect(page.getByText('No hay usuarios que coincidan con los filtros.')).toBeVisible()
  await page.getByLabel('Buscar usuarios').fill('agente@example.com')
  await page.getByRole('button', { name: 'Ver detalle de Agente Demo', exact: true }).click()
})
Then('veo sus datos personales y agencia', async ({ page }) => {
  await expect(dialog(page)).toContainText('Quilmes')
  await expect(dialog(page)).toContainText('Viajes del Sur')
})
Then('no puedo eliminar mi cuenta ni cambiar mi propio rol', async ({ page }) => {
  await expect(page.getByRole('button', { name: 'Eliminar usuario Admin Demo', exact: true })).toBeDisabled()
  await page.getByRole('button', { name: 'Editar usuario Admin Demo', exact: true }).click()
  await expect(page.getByLabel('Rol', { exact: true })).toBeDisabled()
})
Then('veo la Home de viajes sin controles administrativos', async ({ page }) => {
  await expect(page.getByRole('button', { name: 'Administración', exact: true })).toHaveCount(0)
  await expect(page.getByRole('navigation', { name: 'Secciones de administración' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Nuevo usuario', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Nueva agencia', exact: true })).toHaveCount(0)
})
When('el backend rechaza la sesión y su renovación', async ({ page }) => {
  await page.route('**/api/agencias', (route) => route.fulfill({ status: 401, json: { message: 'Authentication is required' } }))
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ status: 401, json: { message: 'Invalid refresh token' } }))
  await page.getByRole('button', { name: 'Agencias', exact: true }).click()
})
Then('vuelvo al login con el aviso de sesión terminada', async ({ page }) => {
  await expect(page.getByRole('heading', { name: 'Iniciar sesión', exact: true })).toBeVisible()
  await expect(page.getByRole('status')).toHaveText('Tu sesión terminó. Iniciá sesión nuevamente.')
})
When('falla temporalmente la consulta de agencias', async ({ page }) => {
  await page.route('**/api/agencias', (route) => route.fulfill({ status: 503, json: { message: 'Servicio temporalmente no disponible' } }))
  await page.getByRole('button', { name: 'Agencias', exact: true }).click()
})
Then('puedo reintentar y ver las agencias', async ({ page }) => {
  await expect(page.getByRole('alert')).toHaveText('Servicio temporalmente no disponible')
  await page.unroute('**/api/agencias')
  await page.getByRole('button', { name: 'Reintentar', exact: true }).click()
  await expect(page.getByRole('row').filter({ hasText: 'sur@example.com' })).toBeVisible()
})
