import type { SesionAutenticada } from '../api/auth'
import { AuthCard } from '../components/auth/AuthCard'
import { LoginForm } from '../components/auth/LoginForm'

type LoginPageProps = { onRegister: () => void; onLoggedIn: (session: SesionAutenticada) => void }

export function LoginPage({ onRegister, onLoggedIn }: LoginPageProps) {
  return <AuthCard title="Iniciar sesión" description="Ingresá tus datos para continuar." footer={
    <>¿No tenés una cuenta? <button className="font-semibold text-sky-700 hover:underline" type="button" onClick={onRegister}>Registrate</button></>
  }>
    <LoginForm onLoggedIn={onLoggedIn} />
  </AuthCard>
}
