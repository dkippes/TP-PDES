export type DiaCalendario = { iso: string; dia: number; enMes: boolean }

const dos = (n: number) => String(n).padStart(2, '0')

/** 'yyyy-MM-dd' from local date parts; going through Date/UTC would shift the day. */
export const aIso = (anio: number, mes: number, dia: number) => `${anio}-${dos(mes + 1)}-${dos(dia)}`

export const hoyIso = (ahora = new Date()) => aIso(ahora.getFullYear(), ahora.getMonth(), ahora.getDate())

/** Year and zero-based month of an ISO date. */
export function mesDe(iso: string): { anio: number; mes: number } {
  const [anio, mes] = iso.split('-').map(Number)
  return { anio, mes: mes - 1 }
}

export function mesAnterior({ anio, mes }: { anio: number; mes: number }) {
  return mes === 0 ? { anio: anio - 1, mes: 11 } : { anio, mes: mes - 1 }
}

export function mesSiguiente({ anio, mes }: { anio: number; mes: number }) {
  return mes === 11 ? { anio: anio + 1, mes: 0 } : { anio, mes: mes + 1 }
}

/** The weeks (Monday first) that cover a month, padded with days of the neighbouring months. */
export function semanasDelMes(anio: number, mes: number): DiaCalendario[][] {
  const primero = new Date(anio, mes, 1)
  const offset = (primero.getDay() + 6) % 7
  const cantidad = Math.ceil((offset + new Date(anio, mes + 1, 0).getDate()) / 7) * 7
  const dias = Array.from({ length: cantidad }, (_, indice) => {
    const fecha = new Date(anio, mes, 1 - offset + indice)
    return { iso: aIso(fecha.getFullYear(), fecha.getMonth(), fecha.getDate()), dia: fecha.getDate(), enMes: fecha.getMonth() === mes }
  })
  return Array.from({ length: cantidad / 7 }, (_, semana) => dias.slice(semana * 7, semana * 7 + 7))
}

/** Click on a day: the first click starts a range, the second ends it; a day before the start restarts it. */
export function elegirDia(desde: string, hasta: string, dia: string): { desde: string; hasta: string } {
  if (desde === '' || hasta !== '' || dia < desde) return { desde: dia, hasta: '' }
  return { desde, hasta: dia }
}
