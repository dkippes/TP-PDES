import { useState, type FormEvent } from 'react'
import type { AgenciasApi } from '../../api/admin'
import type { AgenciaResponseDTO } from '../../types/admin'
import { useAsyncAction } from '../useAsyncAction'

export type AgenciaFormOptions = {
  api: Pick<AgenciasApi, 'crear' | 'editar'>
  agencia: AgenciaResponseDTO | null
  onSaved: () => void
}

export function useAgenciaForm({ api, agencia, onSaved }: AgenciaFormOptions) {
  const [nombre, setNombre] = useState(agencia?.nombre ?? '')
  const [email, setEmail] = useState(agencia?.email ?? '')
  const { busy, error, execute } = useAsyncAction('No se pudo guardar la agencia.')
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await execute(async () => {
      const datos = { nombre: nombre.trim(), email: email.trim() }
      if (agencia) await api.editar(agencia.id, datos)
      else await api.crear(datos)
      onSaved()
    })
  }

  return { nombre, setNombre, email, setEmail, busy, error, submit }
}
