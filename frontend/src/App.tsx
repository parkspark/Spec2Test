import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { useForm } from 'react-hook-form'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { Button } from './components/ui/button'
import { Input } from './components/ui/input'
import './App.css'

export type User = {
  id: number
  email: string
  name: string
  role: 'USER' | 'QA'
}

type ApiResponse<T> = { data: T }

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
  withCredentials: true,
})

const loginSchema = z.object({
  email: z.email('??? ???? ?????.'),
  password: z.string().min(1, '????? ?????.'),
})

type LoginValues = z.infer<typeof loginSchema>

export function LoginForm({
  onLogin,
  errorMessage,
}: {
  onLogin: (values: LoginValues) => Promise<void>
  errorMessage?: string
}) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({ resolver: zodResolver(loginSchema) })

  return (
    <main className="auth-layout">
      <form className="card login-card" onSubmit={handleSubmit(onLogin)} noValidate>
        <div>
          <p className="eyebrow">GAME QA COPILOT</p>
          <h1>Spec2Test</h1>
          <p>??? ??? ?? ??? ???? ?????.</p>
        </div>

        <label>
          ???
          <Input type="email" autoComplete="email" {...register('email')} />
          {errors.email && <span className="error">{errors.email.message}</span>}
        </label>

        <label>
          ????
          <Input type="password" autoComplete="current-password" {...register('password')} />
          {errors.password && <span className="error">{errors.password.message}</span>}
        </label>

        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? '??? ??' : '???'}
        </Button>
        {errorMessage && <p className="error" role="alert">{errorMessage}</p>}
      </form>
    </main>
  )
}

export function ProjectHome({ user }: { user: User }) {
  return (
    <main className="page-shell">
      <header>
        <div>
          <p className="eyebrow">{user.role}</p>
          <h1>????</h1>
        </div>
        {user.role === 'QA' && <Button>? ????</Button>}
      </header>

      <section className="card empty-state" aria-label="???? ??">
        <h2>???? ??</h2>
        <p>???? API? ???? ?????, ??, ???? QA ??? ?????.</p>
      </section>
    </main>
  )
}

function App() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const userQuery = useQuery({
    queryKey: ['me'],
    queryFn: async () => (await api.get<ApiResponse<User>>('/api/auth/me')).data.data,
    retry: false,
  })
  const login = useMutation({
    mutationFn: async (values: LoginValues) =>
      (await api.post<ApiResponse<User>>('/api/auth/login', values)).data.data,
    onSuccess: (user) => {
      queryClient.setQueryData(['me'], user)
      navigate('/projects')
    },
  })

  if (userQuery.isPending) return <main className="loading">?? ?? ??</main>

  return (
    <Routes>
      <Route
        path="/login"
        element={
          userQuery.data ? (
            <Navigate to="/projects" replace />
          ) : (
            <LoginForm
              onLogin={async (values) => { await login.mutateAsync(values) }}
              errorMessage={login.isError ? '??? ?? ????? ?????.' : undefined}
            />
          )
        }
      />
      <Route
        path="/projects"
        element={userQuery.data ? <ProjectHome user={userQuery.data} /> : <Navigate to="/login" replace />}
      />
      <Route path="*" element={<Navigate to={userQuery.data ? '/projects' : '/login'} replace />} />
    </Routes>
  )
}

export default App
