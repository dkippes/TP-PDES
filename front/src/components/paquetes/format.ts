const MONEDA = "USD";
const precio = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: MONEDA,
  maximumFractionDigits: 0,
});
const diaMes = new Intl.DateTimeFormat("es-AR", {
  day: "numeric",
  month: "short",
});

export const normalizarTexto = (texto: string) =>
  texto
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase()
    .trim();

export const formatPrecio = (valor: number) => precio.format(valor);

// 'yyyy-MM-dd' is parsed as a local date; new Date(string) would read it as UTC and shift the day.
export function formatFecha(iso: string) {
  const [anio, mes, dia] = iso.split("-").map(Number);
  return diaMes.format(new Date(anio, mes - 1, dia));
}

export const formatRango = (desde: string, hasta: string) =>
  `${formatFecha(desde)} – ${formatFecha(hasta)}`;

const fechaCompleta = new Intl.DateTimeFormat("es-AR", {
  day: "numeric",
  month: "long",
  year: "numeric",
});

export function formatFechaCompleta(iso: string) {
  const [anio, mes, dia] = iso.split("-").map(Number);
  return fechaCompleta.format(new Date(anio, mes - 1, dia));
}
