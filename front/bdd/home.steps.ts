import { expect } from '@playwright/test'
import { createBdd } from 'playwright-bdd'
import { test } from './auth.steps.ts'

const { Given, When, Then } = createBdd(test)
const paquetes = Array.from({ length: 12 }, (_, index) => ({
  id: index + 1, destino: `Destino ${index + 1}`, origen: 'Buenos Aires', hotel: { nombre: `Hotel ${index + 1}` },
}))

Given('que uso una pantalla móvil', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
})

Given('que visito el catálogo público con {int} paquetes', async ({ page }, count: number) => {
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ status: 401, json: {} }))
  await page.route('**/api/paquetes', (route) => {
    expect(route.request().headers().authorization).toBeUndefined()
    expect(new URL(route.request().url()).search).toBe('')
    return route.fulfill({ json: paquetes.slice(0, count) })
  })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Todos los paquetes' })).toBeVisible()
  await expect(page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Iniciar sesión', exact: true })).toBeEnabled()
})

Then('veo todas las tarjetas con destino, descripción e imagen', async ({ page }) => {
  await expect(page).toHaveTitle('AterrizAR')
  await expect(page.getByRole('article')).toHaveCount(paquetes.length)
  for (const paquete of paquetes) {
    const card = page.getByRole('article').filter({ has: page.getByRole('heading', { name: paquete.destino, exact: true }) })
    await expect(card).toContainText(`Viaje desde ${paquete.origen} a ${paquete.destino}`)
    await expect(card).toContainText(paquete.hotel.nombre)
    const image = card.getByRole('img', { name: 'Ilustración de viaje' })
    await image.scrollIntoViewIfNeeded()
    await expect.poll(() => image.evaluate((element: HTMLImageElement) => element.complete && element.naturalWidth > 0)).toBe(true)
  }
})

When('abro el login y vuelvo con Inicio', async ({ page }) => {
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Iniciar sesión', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Inicio', exact: true }).click()
})

When('vuelvo a Inicio desde Administración', async ({ page }) => {
  await page.getByRole('button', { name: 'Inicio', exact: true }).click()
})

Then('veo el catálogo y puedo volver a Administración', async ({ page }) => {
  await expect(page.getByRole('heading', { name: 'Todos los paquetes' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Inicio', exact: true })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('button', { name: 'Nuevo usuario', exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: 'Administración', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Administración', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Nuevo usuario', exact: true })).toBeEnabled()
})

Then('veo el aviso de que no hay paquetes', async ({ page }) => {
  await expect(page.getByText('Todavía no hay paquetes disponibles.')).toBeVisible()
  await expect(page.getByRole('article')).toHaveCount(0)
})

Given('que falla la carga del catálogo público', async ({ page }) => {
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ status: 401, json: {} }))
  await page.route('**/api/paquetes', (route) => route.fulfill({ status: 503, json: { message: 'Catálogo no disponible' } }))
  await page.goto('/')
  await expect(page.getByRole('alert')).toContainText('Catálogo no disponible')
})

When('reintento la carga del catálogo', async ({ page }) => {
  await page.route('**/api/paquetes', (route) => route.fulfill({ json: paquetes }))
  await page.getByRole('button', { name: 'Reintentar', exact: true }).click()
})

Then('veo la invitación a unirme antes de los paquetes', async ({ page }) => {
  const invitation = page.getByRole('region', { name: 'Registrate o iniciá sesión' })
  await expect(invitation).toBeVisible()
  await expect(invitation).toContainText('Sumate a AterrizAR y encontrá tu próximo viaje.')
  await expect(invitation.getByRole('button', { name: 'Registrarse', exact: true })).toBeEnabled()
  await expect(invitation.getByRole('button', { name: 'Iniciar sesión', exact: true })).toBeEnabled()
  const invitationBounds = await invitation.boundingBox()
  const catalogBounds = await page.getByRole('heading', { name: 'Todos los paquetes' }).boundingBox()
  expect(invitationBounds).not.toBeNull()
  expect(catalogBounds).not.toBeNull()
  expect(invitationBounds!.y + invitationBounds!.height).toBeLessThan(catalogBounds!.y)
})

When('elijo {string} desde la invitación', async ({ page }, action: string) => {
  await page.getByRole('region', { name: 'Registrate o iniciá sesión' }).getByRole('button', { name: action, exact: true }).click()
})

When('vuelvo al catálogo con Inicio', async ({ page }) => {
  await page.getByRole('button', { name: 'Inicio', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Todos los paquetes' })).toBeVisible()
})

Then('no veo la invitación a unirme', async ({ page }) => {
  await expect(page.getByRole('region', { name: 'Registrate o iniciá sesión' })).toHaveCount(0)
})

When('cierro mi sesión', async ({ page }) => {
  await page.route('**/api/auth/logout', (route) => route.fulfill({ status: 204 }))
  await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click()
})

Then('veo el primer paquete sin desplazamiento horizontal', async ({ page }) => {
  await expect(page.getByRole('article').first()).toBeInViewport()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
})
