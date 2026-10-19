import type { FavoritosControls } from '../../types/favorito'

export function FavoritosStatus({ favoritos }: { favoritos: FavoritosControls }) {
  return <>
    {favoritos.loading && <p role="status" className="text-sm text-slate-500">Cargando favoritos...</p>}
    {favoritos.busy && <p role="status" className="sr-only">Actualizando favoritos...</p>}
    {favoritos.error && <div className="rounded-xl border border-red-200 bg-red-50 p-4">
      <p role="alert" className="text-sm text-red-700">No se pudieron cargar tus favoritos. {favoritos.error}</p>
      <button type="button" onClick={favoritos.reload} className="mt-3 font-semibold text-sky-700 hover:text-sky-900">Reintentar favoritos</button>
    </div>}
    {favoritos.actionError && <p role="alert" className="text-sm text-red-700">{favoritos.actionError}</p>}
  </>
}
