import { useRegisterAgenciaForm } from '../../hooks/auth/useRegisterAgenciaForm'
import { FormField } from '../forms/FormField'
import { AuthForm } from './AuthForm'
import { UsuarioFields } from './UsuarioFields'

export function RegisterAgenciaForm({ onRegistered }: Readonly<{ onRegistered: () => void }>) {
  const { usuario, changeUsuario, agencia, changeAgencia, busy, error, submit } = useRegisterAgenciaForm(onRegistered)

  return <AuthForm onSubmit={submit} busy={busy} error={error} submitLabel="Registrar mi agencia" pendingLabel="Registrando...">
    <fieldset className="space-y-4">
      <legend className="text-sm font-semibold text-slate-900">Tu agencia</legend>
      <FormField label="Nombre de la agencia" maxLength={255} value={agencia.nombre} onChange={(event) => changeAgencia('nombre', event.target.value)} required />
      <FormField label="Correo de la agencia" type="email" maxLength={255} value={agencia.email} onChange={(event) => changeAgencia('email', event.target.value)} required />
    </fieldset>
    <fieldset className="space-y-4">
      <legend className="text-sm font-semibold text-slate-900">Tus datos como responsable</legend>
      <UsuarioFields form={usuario} change={changeUsuario} />
    </fieldset>
  </AuthForm>
}
