import { useState } from 'react'
import type { UsuarioAutenticado } from './api/auth'
import { clearSession, readSession, saveSession } from './auth/session'
import { Navbar } from './components/Navbar'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import type { View } from './types'

function App() {
  const [view, setView] = useState<View>('home')
  const [user, setUser] = useState<UsuarioAutenticado | null>(readSession)

  const handleLoggedIn = (loggedInUser: UsuarioAutenticado) => {
    saveSession(loggedInUser)
    setUser(loggedInUser)
    setView('home')
  }

  const handleLogout = () => {
    clearSession()
    setUser(null)
    setView('home')
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800">
      <Navbar onNavigate={setView} user={user} onLogout={handleLogout} />
      <main className="mx-auto max-w-5xl px-5 py-16">
        {view === 'home' && <HomePage onRegister={() => setView('register')} />}
        {view === 'login' && <LoginPage onLoggedIn={handleLoggedIn} onRegister={() => setView('register')} />}
        {view === 'register' && <RegisterPage onRegistered={handleLoggedIn} onLogin={() => setView('login')} />}
      </main>
    </div>
  )
}

export default App
