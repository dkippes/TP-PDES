import { useState } from 'react'
import type { CompraIntento, ComprasApi } from '../api/compras'
import { ApiError } from '../api/http'
import type { CompraResponseDTO } from '../types/compra'
import type { PaqueteResponseDTO } from '../types/paquete'

function mensajeDeError(exception: unknown): string {
  if (exception instanceof ApiError && exception.status === 409) return 'Los vuelos de este paquete ya no tienen lugar disponible.'
  if (exception instanceof ApiError && exception.status === 503) return 'El servicio de vuelos no está disponible. Podés reintentar sin que se cobre dos veces.'
  if (exception instanceof ApiError) return exception.message
  return 'No se pudo completar la compra. Podés reintentar sin que se cobre dos veces.'
}

/** Opening a package starts a new attempt; confirming again after a failure retries that same attempt. */
export function useCompra(api: ComprasApi, onCompra?: (compra: CompraResponseDTO) => void) {
  const [seleccion, setSeleccion] = useState<{ paquete: PaqueteResponseDTO; intento: CompraIntento } | null>(null)
  const [compra, setCompra] = useState<CompraResponseDTO | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const abrir = (paquete: PaqueteResponseDTO) => {
    setCompra(null)
    setError('')
    setSeleccion({ paquete, intento: api.iniciar(paquete.id) })
  }

  const cerrar = () => {
    if (busy) return
    setSeleccion(null)
  }

  const confirmar = async () => {
    if (!seleccion || busy || compra) return
    setBusy(true)
    setError('')
    try {
      const confirmada = await seleccion.intento.confirmar()
      setCompra(confirmada)
      onCompra?.(confirmada)
    } catch (exception) {
      setError(mensajeDeError(exception))
    } finally {
      setBusy(false)
    }
  }

  return { paquete: seleccion?.paquete ?? null, compra, busy, error, abrir, cerrar, confirmar }
}
