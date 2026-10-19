import type { Rol } from '../../types/admin'

export const inputClass = 'mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 outline-none focus:border-sky-600 focus:ring-2 focus:ring-sky-100 disabled:bg-slate-100'
export const primaryClass = 'rounded-lg bg-sky-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-sky-800 disabled:opacity-50'
export const secondaryClass = 'rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50'
export const dangerClass = 'rounded-lg bg-red-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-red-800 disabled:opacity-50'
export const roleLabels: Record<Rol, string> = { ADMINISTRADOR: 'Administrador', AGENTE: 'Agente', COMPRADOR: 'Comprador' }
