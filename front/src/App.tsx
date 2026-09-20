import { type FormEvent, useState } from 'react'
import { Navbar } from './components/Navbar'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import type { View } from './types'

function App() {
  const [view, setView] = useState<View>('home')

  const preventSubmit = (event: FormEvent<HTMLFormElement>) => event.preventDefault()

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800">
      <Navbar onNavigate={setView} />
      <main className="mx-auto max-w-5xl px-5 py-16">
        {view === 'home' && <HomePage onRegister={() => setView('register')} />}
        {view === 'login' && <LoginPage onSubmit={preventSubmit} onRegister={() => setView('register')} />}
        {view === 'register' && <RegisterPage onSubmit={preventSubmit} onLogin={() => setView('login')} />}
      </main>
    </div>
  )
}

export default App
