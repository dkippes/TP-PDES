import type { ReactNode } from 'react'
import { primaryClass } from './controls'

type AdminPanelProps = {
  titleId: string
  title: string
  description: string
  actionLabel: string
  onCreate: () => void
  createDisabled: boolean
  children: ReactNode
}

export function AdminPanel({ titleId, title, description, actionLabel, onCreate, createDisabled, children }: AdminPanelProps) {
  return <section aria-labelledby={titleId} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
    <div className="flex flex-wrap items-start justify-between gap-4">
      <div><h2 id={titleId} className="text-xl font-bold text-slate-900">{title}</h2><p className="mt-1 text-sm text-slate-500">{description}</p></div>
      <button type="button" className={primaryClass} onClick={onCreate} disabled={createDisabled}>{actionLabel}</button>
    </div>
    {children}
  </section>
}
