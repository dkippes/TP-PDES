import type { FormEvent } from 'react'

type RegisterPageProps = {
  onLogin: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

const inputClassName = 'mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-slate-900 outline-none transition focus:border-sky-600 focus:ring-2 focus:ring-sky-100'

export function RegisterPage({ onLogin, onSubmit }: RegisterPageProps) {
  return (
    <section className="mx-auto max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-bold text-slate-900">Crear cuenta</h1>
      <p className="mt-1 text-sm text-slate-600">Completá los datos para registrarte.</p>
      <form className="mt-6 space-y-4" onSubmit={onSubmit}>
        <label className="block text-sm font-medium">Nombre<input className={inputClassName} type="text" placeholder="Tu nombre" required /></label>
        <label className="block text-sm font-medium">Correo electrónico<input className={inputClassName} type="email" placeholder="nombre@correo.com" required /></label>
        <label className="block text-sm font-medium">Contraseña<input className={inputClassName} type="password" placeholder="Elegí una contraseña" required /></label>
        <button className="w-full rounded-md bg-sky-700 py-2.5 font-semibold text-white hover:bg-sky-800" type="submit">Registrarme</button>
      </form>
      <p className="mt-5 text-center text-sm text-slate-600">¿Ya tenés cuenta? <button className="font-semibold text-sky-700 hover:underline" type="button" onClick={onLogin}>Iniciá sesión</button></p>
    </section>
  )
}
