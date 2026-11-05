import { AuthCard } from '../components/auth/AuthCard'
import { RegisterAgenciaForm } from '../components/auth/RegisterAgenciaForm'

type RegisterAgenciaPageProps = { onLogin: () => void; onRegistered: () => void; onRegisterBuyer: () => void }

export function RegisterAgenciaPage({ onLogin, onRegistered, onRegisterBuyer }: Readonly<RegisterAgenciaPageProps>) {
  return <AuthCard title="Registrar mi agencia" description="Creamos tu agencia y tu cuenta de agente para que publiques paquetes y veas tus ventas." footer={
    <>¿Ya tenés una cuenta? <button className="font-semibold text-sky-700 hover:underline" type="button" onClick={onLogin}>Iniciá sesión</button>
      {' · '}<button className="font-semibold text-sky-700 hover:underline" type="button" onClick={onRegisterBuyer}>Soy viajero</button></>
  }>
    <RegisterAgenciaForm onRegistered={onRegistered} />
  </AuthCard>
}
