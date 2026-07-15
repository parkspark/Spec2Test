import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { useState, type ReactNode } from 'react'
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
  analysisId: number
  displayOrder: number
  majorCategory: string
  middleCategory: string
  minorCategory: string
  testItem: string
  status: string
  preconditions: string[]
  testSteps: Array<{ stepNumber: number; action: string; expectedResult?: string }>
  expectedResults: string[]
  evidenceSummary: null | { pageNumber: number | null; sectionTitle: string; evidenceType: string; sourceText: string }
  evidences?: Evidence[]
  notes: string[]
  requiresHumanReview: boolean
  reviewedBy?: number | null
  reviewedAt?: string | null
  rejectionReason?: string | null
}

export type Evidence = {
  evidenceType: 'EXPLICIT' | 'INFERRED' | 'UNSUPPORTED'
  verificationStatus: 'EXACT' | 'PARTIAL' | 'SIMILAR' | 'NOT_FOUND'
  pageNumber: number
  sectionTitle: string
  sourceElementId?: string
  sourceText: string
  sourceElementType: 'TEXT' | 'TABLE' | 'IMAGE' | 'CAPTION'
  boundingBox?: { x: number; y: number; width: number; height: number }
  reason: string
}

type AnalysisJob = { planningDocumentId: number }

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
      p.{evidence.pageNumber ?? '-'} / {evidence.sectionTitle} / {evidence.evidenceType}<br />“{evidence.sourceText}”
    </span>
  ) : <>근거 없음</>
}

export function EvidencePanel({
  document,
  page,
  evidences,
  selectedIndex,
  onSelect,
}: {
  document: PlanningDocument
  page: DocumentPage
  evidences: Evidence[]
  selectedIndex: number
  onSelect: (index: number) => void
}) {
  const evidence = evidences[selectedIndex]
  if (!evidence) return <section className="card evidence-panel"><p>연결된 근거가 없습니다.</p></section>

  return (
    <section className="card evidence-panel" aria-label="Evidence 원문">
      <div className="evidence-selectors" aria-label="근거 선택">
        {evidences.map((item, index) => (
          <Button
            key={`${item.pageNumber}-${item.sourceElementId ?? index}`}
            variant={index === selectedIndex ? 'default' : 'outline'}
            onClick={() => onSelect(index)}
            aria-pressed={index === selectedIndex}
          >
            근거 {index + 1} (p.{item.pageNumber})
          </Button>
        ))}
      </div>

      <div className="evidence-preview">
        <img src={`${apiBaseUrl}${page.imageUrl}`} alt={`${document.title} Evidence ${page.pageNumber} 페이지`} />
        {evidence.boundingBox && <span
          className="evidence-highlight"
          aria-label="근거 영역 하이라이트"
          style={{
            left: `${evidence.boundingBox.x * 100}%`,
            top: `${evidence.boundingBox.y * 100}%`,
            width: `${evidence.boundingBox.width * 100}%`,
            height: `${evidence.boundingBox.height * 100}%`,
          }}
        />}
      </div>

      <dl>
        <dt>페이지</dt><dd>{evidence.pageNumber}</dd>
        <dt>섹션</dt><dd>{evidence.sectionTitle}</dd>
        <dt>근거 유형</dt><dd>{evidence.evidenceType}</dd>
        <dt>원문 검증</dt><dd>{evidence.verificationStatus}</dd>
        <dt>원문</dt><dd className="evidence-source">“{evidence.sourceText}”</dd>
        <dt>선정 이유</dt><dd>{evidence.reason}</dd>
      </dl>
    </section>
  )
}

export function TestCaseSheet({
  items,
  selected,
  role,
  onSelect,
  onApprove,
  onReject,
  evidencePanel,
}: {
  items: TestCaseItem[]
  selected?: TestCaseItem
  role: User['role']
  onSelect: (id: number) => void
  onApprove?: (id: number) => Promise<void>
  onReject?: (id: number, reason: string) => Promise<void>
  evidencePanel?: ReactNode
}) {
  const [reviewAction, setReviewAction] = useState<'approve' | 'reject'>()
  const [rejectionReason, setRejectionReason] = useState('')
  const [reviewError, setReviewError] = useState('')

  const closeReview = () => {
    setReviewAction(undefined)
    setRejectionReason('')
    setReviewError('')
  }

  const submitReview = async () => {
    if (!selected) return
    try {
      if (reviewAction === 'approve') await onApprove?.(selected.id)
      if (reviewAction === 'reject') await onReject?.(selected.id, rejectionReason.trim())
      closeReview()
    } catch {
      setReviewError('승인/반려 처리에 실패했습니다.')
    }
  }

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

      <div className="review-detail-layout">
      <section className="card test-case-detail" aria-label="선택 테스트 케이스 상세">
        {selected ? (
          <>
            <div className="detail-heading">
              <div><p className="eyebrow">TC #{selected.displayOrder}</p><h2>{selected.testItem}</h2></div>
              {role === 'QA' && <div className="review-actions">
                <Button disabled={selected.status !== 'GENERATED' || !onApprove}
                  onClick={() => setReviewAction('approve')}>승인</Button>
                <Button disabled={selected.status !== 'GENERATED' || !onReject} variant="outline"
                  onClick={() => setReviewAction('reject')}>반려</Button>
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
              {selected.reviewedAt && <><dt>검토 정보</dt><dd>사용자 #{selected.reviewedBy} · {new Date(selected.reviewedAt).toLocaleString()}</dd></>}
              {selected.rejectionReason && <><dt>반려 사유</dt><dd>{selected.rejectionReason}</dd></>}
            </dl>
          </>
        ) : <p>행을 선택하면 전체 내용을 확인할 수 있습니다.</p>}
      </section>
      {evidencePanel}
      </div>
      {reviewAction && selected && (
        <div className="review-modal-backdrop">
          <section className="card review-modal" role="dialog" aria-modal="true"
            aria-labelledby="review-modal-title">
            <h2 id="review-modal-title">{reviewAction === 'approve' ? '테스트 케이스 승인' : '테스트 케이스 반려'}</h2>
            <p>“{selected.testItem}”을(를) {reviewAction === 'approve' ? '승인하시겠습니까?' : '반려하시겠습니까?'}</p>
            {reviewAction === 'reject' && (
              <label>반려 사유 (필수)
                <textarea autoFocus required value={rejectionReason}
                  onChange={(event) => setRejectionReason(event.target.value)} />
              </label>
            )}
            {reviewError && <p className="error" role="alert">{reviewError}</p>}
            <div className="review-actions">
              <Button variant="outline" onClick={closeReview}>취소</Button>
              <Button autoFocus={reviewAction === 'approve'}
                disabled={reviewAction === 'reject' && rejectionReason.trim() === ''}
                onClick={submitReview}>{reviewAction === 'approve' ? '승인 확인' : '반려 확인'}</Button>
            </div>
          </section>
        </div>
      )}
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
  const [evidenceIndex, setEvidenceIndex] = useState(0)
  const queryClient = useQueryClient()
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
  const evidenceQuery = useQuery({
    queryKey: ['test-cases', selectedId, 'evidences'],
    queryFn: async () => (await api.get<ApiResponse<Evidence[]>>(`/api/test-cases/${selectedId}/evidences`)).data.data,
    enabled: selectedId !== undefined,
  })
  const analysisQuery = useQuery({
    queryKey: ['analyses', detailQuery.data?.analysisId],
    queryFn: async () => (await api.get<ApiResponse<AnalysisJob>>(`/api/analyses/${detailQuery.data!.analysisId}`)).data.data,
    enabled: detailQuery.data !== undefined,
  })
  const documentQuery = useQuery({
    queryKey: ['documents', analysisQuery.data?.planningDocumentId],
    queryFn: async () => (await api.get<ApiResponse<PlanningDocument>>(`/api/documents/${analysisQuery.data!.planningDocumentId}`)).data.data,
    enabled: analysisQuery.data !== undefined,
  })
  const selectedEvidence = evidenceQuery.data?.[evidenceIndex]
  const pageQuery = useQuery({
    queryKey: ['documents', analysisQuery.data?.planningDocumentId, 'pages', selectedEvidence?.pageNumber],
    queryFn: async () => (await api.get<ApiResponse<DocumentPage>>(
      `/api/documents/${analysisQuery.data!.planningDocumentId}/pages/${selectedEvidence!.pageNumber}`,
    )).data.data,
    enabled: analysisQuery.data !== undefined && selectedEvidence !== undefined,
  })
  const reviewMutation = useMutation({
    mutationFn: async ({ testCaseId, action, reason }: {
      testCaseId: number
      action: 'approve' | 'reject'
      reason?: string
    }) => (await api.post<ApiResponse<TestCaseItem>>(
      `/api/test-cases/${testCaseId}/${action}`,
      action === 'reject' ? { reason } : undefined,
    )).data.data,
    onSuccess: (reviewed) => {
      queryClient.setQueryData(['test-cases', reviewed.id], reviewed)
      void queryClient.invalidateQueries({ queryKey: ['projects', id, 'test-cases'] })
    },
  })

  if (listQuery.isPending) return <main className="loading">테스트 케이스 불러오는 중</main>
  if (listQuery.isError) return <main className="loading">테스트 케이스를 불러오지 못했습니다.</main>
  let evidencePanel: ReactNode
  if (selectedId !== undefined) {
    if (evidenceQuery.isError || analysisQuery.isError || documentQuery.isError || pageQuery.isError) {
      evidencePanel = <section className="card evidence-panel"><p>Evidence 원문을 불러오지 못했습니다.</p></section>
    } else if (evidenceQuery.data?.length === 0) {
      evidencePanel = <section className="card evidence-panel"><p>연결된 근거가 없습니다.</p></section>
    } else if (documentQuery.data && pageQuery.data && evidenceQuery.data) {
      evidencePanel = <EvidencePanel document={documentQuery.data} page={pageQuery.data}
        evidences={evidenceQuery.data} selectedIndex={evidenceIndex} onSelect={setEvidenceIndex} />
    } else {
      evidencePanel = <section className="card evidence-panel"><p>Evidence 원문 불러오는 중</p></section>
    }
  }

  return <TestCaseSheet items={listQuery.data} selected={detailQuery.data} role={user.role}
    onSelect={(testCaseId) => { setEvidenceIndex(0); setSelectedId(testCaseId) }}
    onApprove={(testCaseId) => reviewMutation.mutateAsync({ testCaseId, action: 'approve' }).then(() => undefined)}
    onReject={(testCaseId, reason) => reviewMutation.mutateAsync({ testCaseId, action: 'reject', reason }).then(() => undefined)}
    evidencePanel={evidencePanel} />
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
