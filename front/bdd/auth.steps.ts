import { expect } from '@playwright/test'
import { createBdd, test as base } from 'playwright-bdd'

export const test = base.extend<{ authRequests: string[] }>({
  authRequests: async ({ page }, provide) => {
    const requests: string[] = []
    page.on('request', (request) => {
      if (/\/api\/auth\/(register|login)$/.test(request.url())) requests.push(request.url())
    })
    await provide(requests)
  },
})
const { Given, When, Then } = createBdd(test)
const buyer = {
  id: 1, nombre: 'Ana', apellido: 'Pérez', correo: 'ana@example.com',
  direccion: 'Quilmes', role: 'COMPRADOR',
}
const heading = (form: string) => form === 'registro' ? 'Crear cuenta' : 'Iniciar sesión'

Given('que estoy en el formulario de {string}', async ({ page, authRequests }, form: string) => {
  expect(authRequests).toEqual([])
  // Every API request is intercepted; no request reaches a backend.
  await page.route(/\/api\/auth\/[^/]+$/, async (route) => {
    if (new URL(route.request().url()).pathname === '/api/auth/refresh') {
      await route.fulfill({ status: 401, json: { message: 'Invalid refresh token' } })
    } else {
      await route.abort()
    }
  })
  await page.goto('/')
  await page.getByRole('button', {
    name: form === 'registro' ? 'Registrarse' : 'Iniciar sesión', exact: true,
  }).click()
  await expect(page.getByRole('heading', { name: heading(form), exact: true })).toBeVisible()
})

Given('la API acepta el registro', async ({ page }) => {
  await page.route('**/api/auth/register', async (route) => {
    expect(route.request().postDataJSON()).toEqual({
      nombre: buyer.nombre, apellido: buyer.apellido, correo: buyer.correo,
      direccion: buyer.direccion, password: 'password123',
    })
    await route.fulfill({ status: 201, json: buyer })
  })
})

Given('la API rechaza el registro por correo duplicado', async ({ page }) => {
  await page.route('**/api/auth/register', (route) =>
    route.fulfill({ status: 409, json: { message: 'Email already registered' } }))
})

Given('la API acepta el login', async ({ page }) => {
  await page.route('**/api/auth/login', async (route) => {
    expect(route.request().postDataJSON()).toEqual({ correo: buyer.correo, password: 'password123' })
    await route.fulfill({
      status: 200,
      json: { accessToken: 'test-token', tokenType: 'Bearer', expiresIn: 900, user: buyer },
    })
  })
})

Given('la API rechaza el login por credenciales incorrectas', async ({ page }) => {
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({ status: 401, json: { message: 'Invalid credentials' } }))
})

When('completo el registro con correo {string} y contraseña {string}', async ({ page }, correo: string, password: string) => {
  await page.getByLabel('Nombre', { exact: true }).fill(buyer.nombre)
  await page.getByLabel('Apellido', { exact: true }).fill(buyer.apellido)
  await page.getByLabel('Dirección').fill(buyer.direccion)
  await page.getByLabel('Correo electrónico').fill(correo)
  await page.getByLabel('Contraseña').fill(password)
})

When('completo el login con correo {string} y contraseña {string}', async ({ page }, correo: string, password: string) => {
  await page.getByLabel('Correo electrónico').fill(correo)
  await page.getByLabel('Contraseña').fill(password)
})

When('envío el formulario de {string}', async ({ page }, form: string) => {
  await page.getByRole('button', { name: form === 'registro' ? 'Registrarme' : 'Ingresar', exact: true }).click()
})

Then('veo el formulario de {string}', async ({ page }, form: string) => {
  await expect(page.getByRole('heading', { name: heading(form), exact: true })).toBeVisible()
})

Then('veo el mensaje {string}', async ({ page }, message: string) => {
  await expect(page.getByRole('status')).toHaveText(message)
})

Then('veo el error {string}', async ({ page }, message: string) => {
  await expect(page.getByRole('alert')).toHaveText(message)
})

Then('veo la Home autenticada con el saludo {string}', async ({ page }, greeting: string) => {
  await expect(page.getByText(greeting, { exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Encontrá tu próximo viaje.' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Cerrar sesión', exact: true })).toBeVisible()
})

Then('no veo la opción de cerrar sesión', async ({ page }) => {
  await expect(page.getByRole('button', { name: 'Cerrar sesión', exact: true })).toHaveCount(0)
})

Then('el navegador marca un campo inválido', async ({ page }) => {
  await expect(page.locator('input:invalid').first()).toBeFocused()
})

Then('no se envían solicitudes de autenticación', async ({ authRequests }) => {
  expect(authRequests).toEqual([])
})
