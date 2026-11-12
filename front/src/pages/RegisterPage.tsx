import { AuthCard } from '../components/auth/AuthCard'
import { RegisterForm } from '../components/auth/RegisterForm'

type RegisterPageProps = { onLogin: () => void; onRegistered: () => void; onRegisterAgencia: () => void }

export function RegisterPage({ onLogin, onRegistered, onRegisterAgencia }: RegisterPageProps) {
  return <AuthCard title="Crear cuenta" description="Completá los datos para registrarte." footer={
    <>¿Ya tenés una cuenta? <button className="font-semibold text-sky-700 hover:underline" type="button" onClick={onLogin}>Iniciá sesión</button>
    {' · '}<button className="font-semibold text-sky-700 hover:underline" type="button" onClick={onRegisterAgencia}>Registrar una agencia</button></>
  }>
    <RegisterForm onRegistered={onRegistered} />
  </AuthCard>
}
