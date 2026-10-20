type PaginacionProps = {
  pagina: number
  totalPaginas: number
  onChange: (pagina: number) => void
}

const botonClass = 'rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50'

export function Paginacion({ pagina, totalPaginas, onChange }: Readonly<PaginacionProps>) {
  if (totalPaginas <= 1) return null
  const ir = (destino: number) => {
    window.scrollTo(0, 0)
    onChange(destino)
  }
  return <nav aria-label="Paginación" className="flex items-center justify-center gap-4">
    <button type="button" className={botonClass} disabled={pagina <= 0} onClick={() => ir(pagina - 1)}>Anterior</button>
    <span className="text-sm text-slate-600">Página {pagina + 1} de {totalPaginas}</span>
    <button type="button" className={botonClass} disabled={pagina >= totalPaginas - 1} onClick={() => ir(pagina + 1)}>Siguiente</button>
  </nav>
}
