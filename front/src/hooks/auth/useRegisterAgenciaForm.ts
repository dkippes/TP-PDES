import { useState, type FormEvent } from 'react'
import { registrarAgencia } from '../../api/auth'
import { useAsyncAction } from '../useAsyncAction'

export function useRegisterAgenciaForm(onRegistered: () => void) {
  const [usuario, setUsuario] = useState({ nombre: '', apellido: '', correo: '', direccion: '', password: '' })
  const [agencia, setAgencia] = useState({ nombre: '', email: '' })
  const { busy, error, execute } = useAsyncAction('No se pudo registrar la agencia.')
  const changeUsuario = (field: keyof typeof usuario, value: string) => {
    setUsuario((current) => ({ ...current, [field]: value }))
  }
  const changeAgencia = (field: keyof typeof agencia, value: string) => {
    setAgencia((current) => ({ ...current, [field]: value }))
  }
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await execute(async () => {
      await registrarAgencia({ usuario, agencia: { nombre: agencia.nombre.trim(), email: agencia.email.trim() } })
      onRegistered()
    })
  }

  return { usuario, changeUsuario, agencia, changeAgencia, busy, error, submit }
}
