type FavoritoButtonProps = {
  destino: string
  guardado: boolean
  disabled: boolean
  onToggle: () => void
}

export function FavoritoButton({ destino, guardado, disabled, onToggle }: FavoritoButtonProps) {
  const label = `${guardado ? 'Quitar de' : 'Agregar a'} favoritos: ${destino}`
  return <button type="button" onClick={onToggle} disabled={disabled} aria-label={label} title={label} aria-pressed={guardado}
    className="absolute right-3 top-3 rounded-full bg-white p-2.5 text-rose-600 shadow-sm transition hover:bg-rose-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-600 disabled:cursor-wait disabled:opacity-50">
    <svg viewBox="0 0 24 24" className="h-6 w-6" fill={guardado ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z" />
    </svg>
  </button>
}
