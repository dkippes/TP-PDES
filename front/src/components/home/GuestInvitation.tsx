type GuestInvitationProps = { onRegister: () => void; onLogin: () => void }

export function GuestInvitation({ onRegister, onLogin }: GuestInvitationProps) {
  return <section aria-labelledby="home-invitation-title" className="flex flex-col gap-5 rounded-2xl border border-sky-200 bg-sky-50 p-5 sm:p-6 lg:flex-row lg:items-center lg:justify-between">
    <div>
      <h2 id="home-invitation-title" className="text-2xl font-bold tracking-tight text-slate-900">Registrate o iniciá sesión</h2>
      <p className="mt-2 text-sm text-slate-600">Sumate a AterrizAR y encontrá tu próximo viaje.</p>
    </div>
    <div className="flex flex-col gap-3 sm:flex-row lg:shrink-0">
      <button type="button" onClick={onRegister} className="rounded-lg bg-sky-700 px-5 py-2.5 text-sm font-semibold text-white hover:bg-sky-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-700">Registrarse</button>
      <button type="button" onClick={onLogin} className="rounded-lg border border-sky-700 bg-white px-5 py-2.5 text-sm font-semibold text-sky-700 hover:bg-sky-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-700">Iniciar sesión</button>
    </div>
  </section>
}
