import type { ComponentProps } from 'react'
import { PaquetesList } from './PaquetesList'

export function PaquetesCatalog(props: ComponentProps<typeof PaquetesList>) {
  return <section className="space-y-8">
    <div>
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-700">AterrizAR</p>
      <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Todos los paquetes</h2>
      <p className="mt-3 text-slate-600">Conocé los destinos y elegí tu próximo viaje.</p>
    </div>
    <PaquetesList {...props} />
  </section>
}
