export type AdminSection = 'usuarios' | 'agencias'
type AdminSectionsProps = { section: AdminSection; onSelect: (section: AdminSection) => void }

export function AdminSections({ section, onSelect }: AdminSectionsProps) {
  return <nav aria-label="Secciones de administración" className="flex gap-2">
    {(['usuarios', 'agencias'] as const).map((item) => <button key={item} type="button" aria-pressed={section === item} onClick={() => onSelect(item)}
      className={`rounded-lg px-5 py-3 text-sm font-semibold transition ${section === item ? 'bg-sky-700 text-white' : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-100'}`}>
      {item === 'usuarios' ? 'Usuarios' : 'Agencias'}
    </button>)}
  </nav>
}
