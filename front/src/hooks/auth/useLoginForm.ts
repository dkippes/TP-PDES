import { useState, type FormEvent } from 'react'
import { iniciarSesion, type SesionAutenticada } from '../../api/auth'
import { useAsyncAction } from '../useAsyncAction'

export function useLoginForm(onLoggedIn: (session: SesionAutenticada) => void) {
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const { busy, error, execute } = useAsyncAction('No se pudo iniciar sesión.')

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await execute(async () => { onLoggedIn(await iniciarSesion(correo, password)) })
  }

  return { correo, setCorreo, password, setPassword, busy, error, submit }
}
