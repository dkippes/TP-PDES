import { useState, type FormEvent } from 'react'
import { registrarUsuario } from '../../api/auth'
import { useAsyncAction } from '../useAsyncAction'

export function useRegisterForm(onRegistered: () => void) {
  const [form, setForm] = useState({ nombre: '', apellido: '', correo: '', direccion: '', password: '' })
  const { busy, error, execute } = useAsyncAction('No se pudo crear la cuenta.')
  const change = (field: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
  }
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await execute(async () => {
      await registrarUsuario(form)
      onRegistered()
    })
  }

  return { form, change, busy, error, submit }
}
