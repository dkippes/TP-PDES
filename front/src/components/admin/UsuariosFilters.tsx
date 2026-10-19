import { inputClass, roleLabels } from './controls'

type UsuariosFiltersProps = {
  search: string
  onSearchChange: (search: string) => void
  rol: string
  onRolChange: (rol: string) => void
}

export function UsuariosFilters({ search, onSearchChange, rol, onRolChange }: UsuariosFiltersProps) {
  return <div className="my-6 grid gap-4 sm:grid-cols-[1fr_14rem]">
    <label className="text-sm font-medium">Buscar usuarios<input type="search" className={inputClass} placeholder="Nombre, apellido o correo" value={search} onChange={(event) => onSearchChange(event.target.value)} /></label>
    <label className="text-sm font-medium">Filtrar por rol<select className={inputClass} value={rol} onChange={(event) => onRolChange(event.target.value)}>
      <option value="">Todos los roles</option>
      {Object.entries(roleLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
    </select></label>
  </div>
}
