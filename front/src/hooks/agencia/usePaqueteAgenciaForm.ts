import { useMemo, useState, type SubmitEvent } from 'react'
import type { AgenciaApi } from '../../api/agencia'
import { opcionesIda, opcionesVuelta } from '../../components/agencia/vuelos'
import type { HotelDTO, PaqueteGestionDTO, VueloDTO } from '../../types/agencia'
import { useAsyncAction } from '../useAsyncAction'

export type PaqueteAgenciaFormOptions = {
  api: Pick<AgenciaApi['paquetes'], 'crear' | 'editar'>
  agenciaId: number
  paquete: PaqueteGestionDTO | null
  hoteles: readonly HotelDTO[]
  vuelos: readonly VueloDTO[]
  onSaved: () => void
}

type PaqueteFormState = {
  hotelId: string; vueloIdaId: string; vueloVueltaId: string
  precio: string; fechaInicial: string; fechaFinal: string
}

export function usePaqueteAgenciaForm({ api, agenciaId, paquete, hoteles, vuelos, onSaved }: PaqueteAgenciaFormOptions) {
  const [form, setForm] = useState<PaqueteFormState>({
    hotelId: (paquete?.hotel.id ?? hoteles[0]?.id)?.toString() ?? '',
    vueloIdaId: paquete?.vueloIdaId.toString() ?? '', vueloVueltaId: paquete?.vueloVueltaId.toString() ?? '',
    precio: paquete?.precio.toString() ?? '', fechaInicial: paquete?.fechaInicial ?? '', fechaFinal: paquete?.fechaFinal ?? '',
  })
  const { busy, error, execute } = useAsyncAction('No se pudo guardar el paquete.')
  const change = (field: keyof PaqueteFormState, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
  }
  const fechasElegidas = form.fechaInicial !== '' && form.fechaFinal !== ''
  const rango = { desde: form.fechaInicial, hasta: form.fechaFinal }
  // Days with at least one flight with seats, so the calendar shows where to look.
  const diasConVuelos = useMemo(() => new Set(vuelos.filter((vuelo) => vuelo.disponibilidad > 0).map((vuelo) => vuelo.fecha)), [vuelos])
  const actualIda = paquete?.vueloIdaId ?? null
  const actualVuelta = paquete?.vueloVueltaId ?? null
  const ida = vuelos.find((vuelo) => vuelo.id === Number(form.vueloIdaId))
  const vuelosIda = fechasElegidas ? opcionesIda(vuelos, rango, actualIda) : []
  const vuelosVuelta = fechasElegidas ? opcionesVuelta(vuelos, ida, rango, actualVuelta) : []
  // The route comes from the outbound flight; an edited package keeps its own while that flight is not listed.
  const origen = ida?.origen ?? paquete?.origen ?? ''
  const destino = ida?.destino ?? paquete?.destino ?? ''

  // Changing the dates drops any chosen flight that no longer fits them.
  const cambiarFechas = (fechaInicial: string, fechaFinal: string) => {
    setForm((current) => {
      const next = { ...current, fechaInicial, fechaFinal }
      const valido = fechaInicial !== '' && fechaFinal !== ''
      const rangoNuevo = { desde: next.fechaInicial, hasta: next.fechaFinal }
      const idaNueva = vuelos.find((vuelo) => vuelo.id === Number(next.vueloIdaId))
      const idaValida = valido && opcionesIda(vuelos, rangoNuevo, actualIda).some((vuelo) => vuelo.id === idaNueva?.id)
      if (!idaValida) return { ...next, vueloIdaId: '', vueloVueltaId: '' }
      const vueltaValida = opcionesVuelta(vuelos, idaNueva, rangoNuevo, actualVuelta).some((vuelo) => vuelo.id === Number(next.vueloVueltaId))
      return vueltaValida ? next : { ...next, vueloVueltaId: '' }
    })
  }
  const elegirIda = (id: string) => setForm((current) => ({ ...current, vueloIdaId: id, vueloVueltaId: '' }))
  const completo = fechasElegidas && form.hotelId !== '' && form.precio !== '' && form.vueloIdaId !== '' && form.vueloVueltaId !== ''
  const submit = async (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault()
    await execute(async () => {
      const datos = {
        precio: Number(form.precio), vueloIdaId: Number(form.vueloIdaId), vueloVueltaId: Number(form.vueloVueltaId),
        hotelId: Number(form.hotelId), agenciaId, destino, origen,
        fechaInicial: form.fechaInicial, fechaFinal: form.fechaFinal,
      }
      if (paquete) await api.editar(paquete.id, datos)
      else await api.crear(datos)
      onSaved()
    })
  }

  return { form, change, cambiarFechas, diasConVuelos, elegirIda, ida, vuelosIda, vuelosVuelta, origen, destino, fechasElegidas, completo, busy, error, submit }
}
