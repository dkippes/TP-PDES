import { type FormEvent, useState } from 'react'
import { iniciarSesion, type UsuarioAutenticado } from '../api/auth'

type LoginPageProps = { onRegister: () => void; onLoggedIn: (user: UsuarioAutenticado) => void }

const inputClassName = 'mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-slate-900 outline-none transition focus:border-sky-600 focus:ring-2 focus:ring-sky-100'

export function LoginPage({ onRegister, onLoggedIn }: LoginPageProps) {
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setEnviando(true)
    try {
      const user = await iniciarSesion(correo, password)
      onLoggedIn(user)
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'No se pudo iniciar sesión.')
    } finally {
      setEnviando(false)
    }
  }

  return <section className="mx-auto max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
    <h1 className="text-2xl font-bold text-slate-900">Iniciar sesión</h1>
    <p className="mt-1 text-sm text-slate-600">Ingresá tus datos para continuar.</p>
    <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
      <label className="block text-sm font-medium">Correo electrónico<input className={inputClassName} type="email" placeholder="nombre@correo.com" value={correo} onChange={(event) => setCorreo(event.target.value)} required /></label>
      <label className="block text-sm font-medium">Contraseña<input className={inputClassName} type="password" placeholder="Tu contraseña" value={password} onChange={(event) => setPassword(event.target.value)} required /></label>
      {error && <p className="text-sm text-red-600" role="alert">{error}</p>}
      <button className="w-full rounded-md bg-sky-700 py-2.5 font-semibold text-white hover:bg-sky-800 disabled:bg-slate-400" type="submit" disabled={enviando}>{enviando ? 'Ingresando...' : 'Ingresar'}</button>
    </form>
    <p className="mt-5 text-center text-sm text-slate-600">¿No tenés una cuenta? <button className="font-semibold text-sky-700 hover:underline" type="button" onClick={onRegister}>Registrate</button></p>
  </section>
}
