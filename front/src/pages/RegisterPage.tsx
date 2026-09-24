import { type FormEvent, useState } from 'react'
import { registrarUsuario } from '../api/auth'

type RegisterPageProps = { onLogin: () => void }

const inputClassName = 'mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-slate-900 outline-none transition focus:border-sky-600 focus:ring-2 focus:ring-sky-100'

export function RegisterPage({ onLogin }: RegisterPageProps) {
  const [form, setForm] = useState({ nombre: '', apellido: '', correo: '', direccion: '', password: '' })
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const actualizar = (campo: keyof typeof form, valor: string) => setForm({ ...form, [campo]: valor })

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setEnviando(true)
    try {
      await registrarUsuario(form)
      onLogin()
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No se pudo crear la cuenta.')
    } finally {
      setEnviando(false)
    }
  }

  return <section className="mx-auto max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
    <h1 className="text-2xl font-bold text-slate-900">Crear cuenta</h1>
    <p className="mt-1 text-sm text-slate-600">Completá los datos para registrarte.</p>
    <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
      <label className="block text-sm font-medium">Nombre<input className={inputClassName} value={form.nombre} onChange={(event) => actualizar('nombre', event.target.value)} required /></label>
      <label className="block text-sm font-medium">Apellido<input className={inputClassName} value={form.apellido} onChange={(event) => actualizar('apellido', event.target.value)} required /></label>
      <label className="block text-sm font-medium">Correo electrónico<input className={inputClassName} type="email" value={form.correo} onChange={(event) => actualizar('correo', event.target.value)} required /></label>
      <label className="block text-sm font-medium">Dirección<input className={inputClassName} value={form.direccion} onChange={(event) => actualizar('direccion', event.target.value)} required /></label>
      <label className="block text-sm font-medium">Contraseña<input className={inputClassName} type="password" minLength={4} value={form.password} onChange={(event) => actualizar('password', event.target.value)} required /></label>
      {error && <p className="text-sm text-red-600" role="alert">{error}</p>}
      <button className="w-full rounded-md bg-sky-700 py-2.5 font-semibold text-white hover:bg-sky-800 disabled:bg-slate-400" type="submit" disabled={enviando}>{enviando ? 'Registrando...' : 'Registrarme'}</button>
    </form>
    <p className="mt-5 text-center text-sm text-slate-600">¿Ya tenés una cuenta? <button className="font-semibold text-sky-700 hover:underline" type="button" onClick={onLogin}>Iniciá sesión</button></p>
  </section>
}
