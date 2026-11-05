export type AgenciaSection = 'paquetes' | 'ventas'
type AgenciaSectionsProps = { section: AgenciaSection; onSelect: (section: AgenciaSection) => void }

const etiquetas: Record<AgenciaSection, string> = { paquetes: 'Mis paquetes', ventas: 'Ventas y clientes' }

export function AgenciaSections({ section, onSelect }: Readonly<AgenciaSectionsProps>) {
  return <nav aria-label="Secciones de la agencia" className="flex flex-wrap gap-2">
    {(Object.keys(etiquetas) as AgenciaSection[]).map((item) => <button key={item} type="button" aria-pressed={section === item} onClick={() => onSelect(item)}
      className={`rounded-lg px-5 py-3 text-sm font-semibold transition ${section === item ? 'bg-sky-700 text-white' : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-100'}`}>
      {etiquetas[item]}
    </button>)}
  </nav>
}
