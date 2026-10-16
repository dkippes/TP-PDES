import type { ComponentProps, ReactNode } from 'react'

type FormFieldProps = ComponentProps<'input'> & { label: ReactNode }
const defaultInputClass = 'mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-slate-900 outline-none transition focus:border-sky-600 focus:ring-2 focus:ring-sky-100'

export function FormField({ label, className = defaultInputClass, ...inputProps }: FormFieldProps) {
  return <label className="block text-sm font-medium">
    {label}
    <input {...inputProps} className={className} />
  </label>
}
