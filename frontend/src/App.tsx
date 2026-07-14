import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom'
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

export type PlanningDocument = {
  id: number
  title: string
  originalFileName: string
  pageCount: number
  processingStatus: 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED'
  createdBy: number
  createdAt: string
}

export type DocumentPage = {
  pageNumber: number
  imageUrl: string
  elements: Array<{
    elementId: string
    elementType: string
    text: string
    boundingBox: { x: number; y: number; width: number; height: number }
  }>
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
  withCredentials: true,
})

const apiBaseUrl = String(api.defaults.baseURL).replace(/\/$/, '')

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

export function DocumentViewer({
  document,
  page,
  onPageChange,
}: {
  document: PlanningDocument
  page: DocumentPage
  onPageChange: (pageNumber: number) => void
}) {
  return (
    <main className="document-shell">
      <aside className="card page-list" aria-label="PDF 페이지 목록">
        <h2>페이지</h2>
        {Array.from({ length: document.pageCount }, (_, index) => index + 1).map((pageNumber) => (
          <Button
            key={pageNumber}
            variant={page.pageNumber === pageNumber ? 'default' : 'outline'}
            onClick={() => onPageChange(pageNumber)}
            aria-current={page.pageNumber === pageNumber ? 'page' : undefined}
          >
            {pageNumber} 페이지
          </Button>
        ))}
      </aside>

      <section className="card document-preview">
        <h1>{document.title}</h1>
        <img src={`${apiBaseUrl}${page.imageUrl}`} alt={`${document.title} ${page.pageNumber} 페이지`} />
        <h2>추출 텍스트</h2>
        {page.elements.map((element) => (
          <article key={element.elementId} className="text-element">
            <p>{element.text}</p>
            <small>
              {element.elementId} · x {element.boundingBox.x.toFixed(3)} · y {element.boundingBox.y.toFixed(3)}
            </small>
          </article>
        ))}
      </section>

      <aside className="card document-info">
        <h2>문서 정보</h2>
        <dl>
          <dt>파일명</dt><dd>{document.originalFileName}</dd>
          <dt>페이지 수</dt><dd>{document.pageCount}</dd>
          <dt>업로드 사용자</dt><dd>#{document.createdBy}</dd>
          <dt>업로드 시각</dt><dd>{new Date(document.createdAt).toLocaleString()}</dd>
          <dt>처리 상태</dt><dd>{document.processingStatus}</dd>
          <dt>최근 분석 상태</dt><dd>미실행</dd>
        </dl>
      </aside>
    </main>
  )
}

function DocumentScreen() {
  const { documentId } = useParams()
  const id = Number(documentId)
  const [pageNumber, setPageNumber] = useState(1)
  const documentQuery = useQuery({
    queryKey: ['documents', id],
    queryFn: async () => (await api.get<ApiResponse<PlanningDocument>>(`/api/documents/${id}`)).data.data,
    enabled: Number.isInteger(id),
  })
  const pageQuery = useQuery({
    queryKey: ['documents', id, 'pages', pageNumber],
    queryFn: async () => (await api.get<ApiResponse<DocumentPage>>(`/api/documents/${id}/pages/${pageNumber}`)).data.data,
    enabled: Number.isInteger(id),
  })

  if (documentQuery.isPending || pageQuery.isPending) return <main className="loading">문서 불러오는 중</main>
  if (documentQuery.isError || pageQuery.isError) return <main className="loading">문서를 불러오지 못했습니다.</main>
  return <DocumentViewer document={documentQuery.data} page={pageQuery.data} onPageChange={setPageNumber} />
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
      <Route
        path="/documents/:documentId"
        element={userQuery.data ? <DocumentScreen /> : <Navigate to="/login" replace />}
      />
      <Route path="*" element={<Navigate to={userQuery.data ? '/projects' : '/login'} replace />} />
    </Routes>
  )
}

export default App
