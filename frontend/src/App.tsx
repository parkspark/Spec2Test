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

export type TestCaseItem = {
  id: number
  displayOrder: number
  majorCategory: string
  middleCategory: string
  minorCategory: string
  testItem: string
  status: string
  preconditions: string[]
  testSteps: Array<{ stepNumber: number; action: string; expectedResult?: string }>
  expectedResults: string[]
  evidenceSummary: null | { pageNumber: number | null; evidenceType: string; sourceText: string }
  notes: string[]
  requiresHumanReview: boolean
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

function TextList({ items }: { items: string[] }) {
  if (items.length === 0) return <>-</>
  return <ol>{items.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}</ol>
}

function NoteTags({ testCase }: { testCase: TestCaseItem }) {
  const tags = [...testCase.notes]
  if (testCase.evidenceSummary?.evidenceType === 'UNSUPPORTED' && !tags.some((tag) => tag.includes('근거 없음'))) {
    tags.push('근거 없음')
  }
  if (testCase.requiresHumanReview && !tags.some((tag) => tag.includes('QA'))) tags.push('QA 검토 필수')
  return tags.length ? <div className="note-tags">{tags.map((tag) => <span key={tag}>{tag}</span>)}</div> : <>-</>
}

function EvidenceSummary({ evidence }: { evidence: TestCaseItem['evidenceSummary'] }) {
  return evidence ? (
    <span className="evidence-summary">
      p.{evidence.pageNumber ?? '-'} / {evidence.evidenceType}<br />“{evidence.sourceText}”
    </span>
  ) : <>근거 없음</>
}

export function TestCaseSheet({
  items,
  selected,
  role,
  onSelect,
}: {
  items: TestCaseItem[]
  selected?: TestCaseItem
  role: User['role']
  onSelect: (id: number) => void
}) {
  return (
    <main className="test-sheet-shell">
      <header>
        <p className="eyebrow">TEST CASE REVIEW</p>
        <h1>테스트 시트</h1>
      </header>

      <section className="card table-scroll" aria-label="테스트 케이스 10컬럼 표">
        <table className="test-case-table">
          <thead><tr>
            {['No', '대분류', '중분류', '소분류', '테스트 항목', '사전조건', '테스트 스텝', '기대결과', '기획서 원문 근거', '비고'].map((column) => <th key={column}>{column}</th>)}
          </tr></thead>
          <tbody>
            {items.map((testCase) => (
              <tr
                key={testCase.id}
                tabIndex={0}
                aria-selected={selected?.id === testCase.id}
                onClick={() => onSelect(testCase.id)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') onSelect(testCase.id)
                }}
              >
                <td>{testCase.displayOrder}</td>
                <td>{testCase.majorCategory}</td>
                <td>{testCase.middleCategory}</td>
                <td>{testCase.minorCategory}</td>
                <td>{testCase.testItem}</td>
                <td><TextList items={testCase.preconditions} /></td>
                <td><TextList items={testCase.testSteps.map((step) => `${step.action}${step.expectedResult ? ` → ${step.expectedResult}` : ''}`)} /></td>
                <td><TextList items={testCase.expectedResults} /></td>
                <td><EvidenceSummary evidence={testCase.evidenceSummary} /></td>
                <td><NoteTags testCase={testCase} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="card test-case-detail" aria-label="선택 테스트 케이스 상세">
        {selected ? (
          <>
            <div className="detail-heading">
              <div><p className="eyebrow">TC #{selected.displayOrder}</p><h2>{selected.testItem}</h2></div>
              {role === 'QA' && <div className="review-actions">
                <Button disabled title="T-23에서 승인 기능을 활성화합니다">승인</Button>
                <Button disabled variant="outline" title="T-23에서 반려 기능을 활성화합니다">반려</Button>
              </div>}
            </div>
            <dl>
              <dt>분류</dt><dd>{selected.majorCategory} / {selected.middleCategory} / {selected.minorCategory}</dd>
              <dt>사전조건</dt><dd><TextList items={selected.preconditions} /></dd>
              <dt>테스트 스텝</dt><dd><TextList items={selected.testSteps.map((step) => `${step.action}${step.expectedResult ? ` → ${step.expectedResult}` : ''}`)} /></dd>
              <dt>기대결과</dt><dd><TextList items={selected.expectedResults} /></dd>
              <dt>원문 근거</dt><dd><EvidenceSummary evidence={selected.evidenceSummary} /></dd>
              <dt>비고</dt><dd><NoteTags testCase={selected} /></dd>
              <dt>상태</dt><dd>{selected.status}</dd>
            </dl>
          </>
        ) : <p>행을 선택하면 전체 내용을 확인할 수 있습니다.</p>}
      </section>
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

function TestCaseScreen({ user }: { user: User }) {
  const { projectId } = useParams()
  const id = Number(projectId)
  const [selectedId, setSelectedId] = useState<number>()
  const listQuery = useQuery({
    queryKey: ['projects', id, 'test-cases'],
    queryFn: async () => (await api.get<ApiResponse<{ items: TestCaseItem[] }>>(`/api/projects/${id}/test-cases`)).data.data.items,
    enabled: Number.isInteger(id),
  })
  const detailQuery = useQuery({
    queryKey: ['test-cases', selectedId],
    queryFn: async () => (await api.get<ApiResponse<TestCaseItem>>(`/api/test-cases/${selectedId}`)).data.data,
    enabled: selectedId !== undefined,
  })

  if (listQuery.isPending) return <main className="loading">테스트 케이스 불러오는 중</main>
  if (listQuery.isError) return <main className="loading">테스트 케이스를 불러오지 못했습니다.</main>
  return <TestCaseSheet items={listQuery.data} selected={detailQuery.data} role={user.role} onSelect={setSelectedId} />
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
      <Route
        path="/projects/:projectId/test-cases"
        element={userQuery.data ? <TestCaseScreen user={userQuery.data} /> : <Navigate to="/login" replace />}
      />
      <Route path="*" element={<Navigate to={userQuery.data ? '/projects' : '/login'} replace />} />
    </Routes>
  )
}

export default App
