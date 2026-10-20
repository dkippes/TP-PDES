import type { ReactNode } from 'react'

type AppLayoutProps = { navigation: ReactNode; message: string; restoring: boolean; children: ReactNode }

export function AppLayout({ navigation, message, restoring, children }: AppLayoutProps) {
  return <div className="min-h-screen bg-slate-50 text-slate-800">
    {navigation}
    <main className="mx-auto max-w-6xl px-5 py-10 sm:py-14">
      {message && <p className="mb-4 text-sm text-slate-700" role="status">{message}</p>}
      {restoring ? <p role="status">Recuperando sesión...</p> : children}
    </main>
  </div>
}
