type HomePageProps = {
  onRegister: () => void
}

export function HomePage({ onRegister }: HomePageProps) {
  return (
    <section className="py-16 text-center">
      <p className="mb-3 text-sm font-semibold uppercase tracking-widest text-sky-700">AterrizAR</p>
      <h1 className="mx-auto max-w-2xl text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl">Encontrá tu próximo viaje.</h1>
      <p className="mx-auto mt-5 max-w-xl text-lg text-slate-600">Una forma simple de buscar y organizar paquetes turísticos.</p>
      <button className="mt-8 rounded-md bg-sky-700 px-5 py-3 font-semibold text-white hover:bg-sky-800" type="button" onClick={onRegister}>Crear una cuenta</button>
    </section>
  )
}
