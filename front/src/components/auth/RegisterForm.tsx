import { useRegisterForm } from '../../hooks/auth/useRegisterForm'
import { AuthForm } from './AuthForm'
import { UsuarioFields } from './UsuarioFields'

export function RegisterForm({ onRegistered }: { onRegistered: () => void }) {
  const { form, change, busy, error, submit } = useRegisterForm(onRegistered)

  return <AuthForm onSubmit={submit} busy={busy} error={error} submitLabel="Registrarme" pendingLabel="Registrando...">
    <UsuarioFields form={form} change={change} />
  </AuthForm>
}
