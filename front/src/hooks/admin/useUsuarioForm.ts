import { useState, type FormEvent } from 'react'
import type { UsuariosApi } from '../../api/admin'
import type { Rol, UsuarioResponseDTO } from '../../types/admin'
import { useAsyncAction } from '../useAsyncAction'

export type UsuarioFormOptions = {
  api: Pick<UsuariosApi, 'crear' | 'editar'>
  usuario: UsuarioResponseDTO | null
  currentUserId: number
  onSaved: (usuario: UsuarioResponseDTO, passwordChanged: boolean) => void
}

type UsuarioFormState = {
  nombre: string; apellido: string; correo: string; direccion: string
  rol: Rol; agenciaId: string; password: string
}

export function useUsuarioForm({ api, usuario, currentUserId, onSaved }: UsuarioFormOptions) {
  const [form, setForm] = useState<UsuarioFormState>({
    nombre: usuario?.nombre ?? '', apellido: usuario?.apellido ?? '', correo: usuario?.correo ?? '',
    direccion: usuario?.direccion ?? '', rol: usuario?.rol ?? 'COMPRADOR',
    agenciaId: usuario?.agenciaId?.toString() ?? '', password: '',
  })
  const { busy, error, execute } = useAsyncAction('No se pudo guardar el usuario.')
  function change<K extends keyof UsuarioFormState>(field: K, value: UsuarioFormState[K]) {
    setForm((current) => ({ ...current, [field]: value }))
  }
  const isSelf = usuario?.id === currentUserId
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await execute(async () => {
      const datos = {
        nombre: form.nombre.trim(), apellido: form.apellido.trim(), correo: form.correo.trim(),
        direccion: form.direccion.trim(), rol: form.rol,
        agenciaId: form.rol === 'AGENTE' ? Number(form.agenciaId) : null,
      }
      const saved = usuario
        ? await api.editar(usuario.id, { ...datos, ...(form.password ? { password: form.password } : {}) })
        : await api.crear({ ...datos, password: form.password })
      onSaved(saved, usuario !== null && form.password !== '')
    })
  }

  return { form, change, isSelf, busy, error, submit }
}
