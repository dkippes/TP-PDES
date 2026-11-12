const secciones = [
  { id: 'usuarios', label: 'Usuarios' },
  { id: 'agencias', label: 'Agencias' },
  { id: 'reportes', label: 'Reportes' },
] as const

export type AdminSection = (typeof secciones)[number]['id']
type AdminSectionsProps = { section: AdminSection; onSelect: (section: AdminSection) => void }

export function AdminSections({ section, onSelect }: AdminSectionsProps) {
  return <nav aria-label="Secciones de administración" className="flex flex-wrap gap-2">
    {secciones.map(({ id, label }) => <button key={id} type="button" aria-pressed={section === id} onClick={() => onSelect(id)}
      className={`rounded-lg px-5 py-3 text-sm font-semibold transition ${section === id ? 'bg-sky-700 text-white' : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-100'}`}>
      {label}
    </button>)}
  </nav>
}
