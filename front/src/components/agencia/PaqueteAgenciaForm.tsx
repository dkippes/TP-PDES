import { usePaqueteAgenciaForm, type PaqueteAgenciaFormOptions } from '../../hooks/agencia/usePaqueteAgenciaForm'
import type { VueloDTO } from '../../types/agencia'
import { AdminFormActions } from '../admin/AdminFormActions'
import { inputClass } from '../admin/controls'
import { FormField } from '../forms/FormField'
import { DateRangePicker } from '../forms/DateRangePicker'
import { Select, type SelectOption } from '../forms/Select'
import { Modal } from '../Modal'
import { opcionVuelo } from './vuelos'

type Props = PaqueteAgenciaFormOptions & { onClose: () => void }

export function PaqueteAgenciaForm({ onClose, ...options }: Readonly<Props>) {
  const { form, change, cambiarFechas, diasConVuelos, elegirIda, ida, vuelosIda, vuelosVuelta, origen, destino, fechasElegidas, completo, busy, error, submit } = usePaqueteAgenciaForm(options)
  const { paquete, hoteles } = options
  const hotelOptions = hoteles.map((hotel) => ({ value: String(hotel.id), label: hotel.nombre, detalle: hotel.pais }))

  return <Modal title={paquete ? 'Editar paquete' : 'Nuevo paquete'} busy={busy} onClose={onClose}>
    <form onSubmit={submit} className="space-y-4">
      <fieldset disabled={busy} className="space-y-4">
        <DateRangePicker label="Fechas del viaje" desde={form.fechaInicial} hasta={form.fechaFinal} onChange={cambiarFechas} marcados={diasConVuelos} />
        <Select label="Vuelo de ida" value={form.vueloIdaId} onChange={elegirIda} options={vuelosOptions(vuelosIda, paquete?.vueloIdaId ?? null)}
          disabled={!fechasElegidas} placeholder={fechasElegidas ? 'Elegí el vuelo de ida' : 'Primero elegí las fechas'}
          emptyMessage="No hay vuelos con lugares dentro de esas fechas." />
        <Select label="Vuelo de vuelta" value={form.vueloVueltaId} onChange={(value) => change('vueloVueltaId', value)} options={vuelosOptions(vuelosVuelta, paquete?.vueloVueltaId ?? null)}
          disabled={!fechasElegidas || !ida} placeholder={ida ? 'Elegí el vuelo de vuelta' : 'Primero elegí el vuelo de ida'}
          emptyMessage={ida ? `No hay vuelos de ${ida.destino} a ${ida.origen} con lugares dentro de esas fechas.` : undefined} />
        {origen && <p className="rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-600">Viaje: <strong className="text-slate-900">{origen} → {destino}</strong></p>}
        <Select label="Hotel" value={form.hotelId} onChange={(value) => change('hotelId', value)} options={hotelOptions}
          placeholder="Elegí un hotel" emptyMessage="No hay hoteles disponibles." />
        <FormField label="Precio (USD)" className={inputClass} type="number" min={1} step="any" value={form.precio} onChange={(event) => change('precio', event.target.value)} required />
      </fieldset>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      <AdminFormActions busy={busy} disabled={!completo} onCancel={onClose} submitLabel="Guardar paquete" />
    </form>
  </Modal>
}

/** A package edited after its flight left the list keeps it selectable as a plain id. */
function vuelosOptions(vuelos: readonly VueloDTO[], actual: number | null): SelectOption[] {
  const options = vuelos.map(opcionVuelo)
  return actual !== null && !vuelos.some((vuelo) => vuelo.id === actual) ? [{ value: String(actual), label: `Vuelo actual (#${actual})` }, ...options] : options
}
