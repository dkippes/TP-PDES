import type { FormEvent, ReactNode } from 'react'

type AuthFormProps = {
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  children: ReactNode
  busy: boolean
  error: string
  submitLabel: string
  pendingLabel: string
}

export function AuthForm({ onSubmit, children, busy, error, submitLabel, pendingLabel }: AuthFormProps) {
  return <form className="mt-6 space-y-4" onSubmit={onSubmit}>
    {children}
    {error && <p className="text-sm text-red-600" role="alert">{error}</p>}
    <button className="w-full rounded-md bg-sky-700 py-2.5 font-semibold text-white hover:bg-sky-800 disabled:bg-slate-400" type="submit" disabled={busy}>
      {busy ? pendingLabel : submitLabel}
    </button>
  </form>
}
