import type { ReactNode } from 'react'

type AuthCardProps = { title: string; description: string; children: ReactNode; footer: ReactNode }

export function AuthCard({ title, description, children, footer }: AuthCardProps) {
  return <section className="mx-auto max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
    <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
    <p className="mt-1 text-sm text-slate-600">{description}</p>
    {children}
    <p className="mt-5 text-center text-sm text-slate-600">{footer}</p>
  </section>
}
