import { useRegisterForm } from '../../hooks/auth/useRegisterForm'
import { FormField } from '../forms/FormField'
import { AuthForm } from './AuthForm'

export function RegisterForm({ onRegistered }: { onRegistered: () => void }) {
  const { form, change, busy, error, submit } = useRegisterForm(onRegistered)

  return <AuthForm onSubmit={submit} busy={busy} error={error} submitLabel="Registrarme" pendingLabel="Registrando...">
    <FormField label="Nombre" autoComplete="given-name" maxLength={255} value={form.nombre} onChange={(event) => change('nombre', event.target.value)} required />
    <FormField label="Apellido" autoComplete="family-name" maxLength={255} value={form.apellido} onChange={(event) => change('apellido', event.target.value)} required />
    <FormField label="Correo electrónico" type="email" autoComplete="email" maxLength={255} value={form.correo} onChange={(event) => change('correo', event.target.value)} required />
    <FormField label="Dirección" autoComplete="street-address" maxLength={255} value={form.direccion} onChange={(event) => change('direccion', event.target.value)} required />
    <FormField label="Contraseña" type="password" autoComplete="new-password" minLength={8} maxLength={72} value={form.password} onChange={(event) => change('password', event.target.value)} required />
  </AuthForm>
}
