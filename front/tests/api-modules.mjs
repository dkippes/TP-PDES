import { readFile } from 'node:fs/promises'
import ts from 'typescript'

// Compile the real modules once per test process, preserving their shared refresh state.
async function moduleUrl(path, imports = {}) {
  const source = await readFile(new URL(`../src/${path}`, import.meta.url), 'utf8')
  let compiled = ts.transpileModule(source, {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 },
  }).outputText
  for (const [specifier, url] of Object.entries(imports)) {
    compiled = compiled.replaceAll(`'${specifier}'`, JSON.stringify(url))
  }
  return 'data:text/javascript;base64,' + Buffer.from(compiled).toString('base64')
}

const httpUrl = await moduleUrl('api/http.ts')
const authUrl = await moduleUrl('api/auth.ts', { './http': httpUrl })
const sessionUrl = await moduleUrl('auth/session.ts', { '../api/auth': authUrl })
const authenticatedUrl = await moduleUrl('api/authenticated.ts', { './http': httpUrl, '../auth/session': sessionUrl })

export const auth = await import(authUrl)
export const session = await import(sessionUrl)
export const { createAuthenticatedRequest } = await import(authenticatedUrl)
export const { createAdminApi } = await import(await moduleUrl('api/admin.ts'))
export const { requestJson } = await import(httpUrl)
export const { createPaquetesApi } = await import(await moduleUrl('api/paquetes.ts'))
export const { createFavoritosApi } = await import(await moduleUrl('api/favoritos.ts'))
