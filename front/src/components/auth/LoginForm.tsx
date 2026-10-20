import type { SesionAutenticada } from '../../api/auth'
import { useLoginForm } from '../../hooks/auth/useLoginForm'
import { FormField } from '../forms/FormField'
import { AuthForm } from './AuthForm'

export function LoginForm({ onLoggedIn }: { onLoggedIn: (session: SesionAutenticada) => void }) {
  const { correo, setCorreo, password, setPassword, busy, error, submit } = useLoginForm(onLoggedIn)

  return <AuthForm onSubmit={submit} busy={busy} error={error} submitLabel="Ingresar" pendingLabel="Ingresando...">
    <FormField label="Correo electrónico" type="email" autoComplete="email" maxLength={255} placeholder="nombre@correo.com" value={correo} onChange={(event) => setCorreo(event.target.value)} required />
    <FormField label="Contraseña" type="password" autoComplete="current-password" minLength={8} maxLength={72} placeholder="Tu contraseña" value={password} onChange={(event) => setPassword(event.target.value)} required />
  </AuthForm>
}
