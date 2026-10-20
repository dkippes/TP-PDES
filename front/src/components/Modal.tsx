import { useEffect, useId, useRef, type ReactNode } from 'react'

type Props = { title: string; children: ReactNode; busy: boolean; onClose: () => void }

export function Modal({ title, children, busy, onClose }: Props) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  useEffect(() => {
    const dialog = ref.current
    dialog?.showModal()
    return () => { dialog?.close() }
  }, [])

  return <dialog ref={ref} aria-labelledby={titleId}
    onCancel={(event) => { event.preventDefault(); if (!busy) onClose() }}
    className="m-auto max-h-[90dvh] w-[calc(100%-2rem)] max-w-lg overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 text-slate-800 shadow-xl backdrop:bg-slate-950/40">
    <div className="mb-6 flex items-start justify-between gap-4">
      <h2 id={titleId} className="text-xl font-bold text-slate-900">{title}</h2>
      <button type="button" onClick={onClose} disabled={busy} aria-label="Cerrar ventana"
        className="rounded-md px-2 text-xl text-slate-500 hover:bg-slate-100 disabled:opacity-50">×</button>
    </div>
    {children}
  </dialog>
}
