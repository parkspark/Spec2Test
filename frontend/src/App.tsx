import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
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

export type Project = {
  id: number
  name: string
  description: string | null
  gameGenre: string | null
  platform: string | null
  documentCount: number
  generatedCount: number
  approvedCount: number
  rejectedCount: number
  openAmbiguityCount: number | null
  lastAnalyzedAt: string | null
}

export type PlanningDocument = {
  id: number
  title: string
  originalFileName: string
  pageCount: number
  processingStatus: 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED'
  createdBy: number
  createdAt: string
  failureReason?: string | null
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
  requirementId: number
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

export type AnalysisJob = {
  id: number
  planningDocumentId: number
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  requestedAt: string
  completedAt: string | null
  failureReason?: string | null
}

export type ProjectDocument = PlanningDocument & { analysis: AnalysisJob | null }

export type Output = {
  id: number
  outputType: 'CSV_EXPORT' | 'MARKDOWN_EXPORT'
  status: 'PENDING' | 'SUCCESS' | 'FAILED'
  finalContent: string | null
  fileName: string
  failureReason: string | null
}

export type LoopLog = {
  id: number
  depthStep: number
  generatedDraft: string
  evaluationScore: number
  evaluationFeedback: string
  createdAt: string
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

const projectSchema = z.object({
  name: z.string().trim().min(1, '프로젝트명을 입력해 주세요.'),
  description: z.string(),
  gameGenre: z.string(),
  platform: z.string(),
})

type ProjectValues = z.infer<typeof projectSchema>

const uploadSchema = z.object({
  title: z.string().trim().min(1, '기획서 제목을 입력해 주세요.'),
  file: z.custom<FileList>((files) => Boolean((files as FileList | undefined)?.length), 'PDF 파일을 선택해 주세요.')
    .refine((files) => files[0]?.type === 'application/pdf', 'PDF 파일만 업로드할 수 있습니다.'),
  analyzeAfterUpload: z.boolean(),
})

type UploadValues = z.infer<typeof uploadSchema>

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

export function ProjectHome({
  user,
  projects = [],
  onCreate = () => undefined,
  onOpen = () => undefined,
}: {
  user: User
  projects?: Project[]
  onCreate?: () => void
  onOpen?: (id: number) => void
}) {
  return (
    <main className="page-shell">
      <header>
        <div>
          <p className="eyebrow">{user.role}</p>
          <h1>프로젝트</h1>
          <p>게임 기획서 분석과 테스트 케이스 현황을 확인하세요.</p>
        </div>
        {user.role === 'QA' && <Button onClick={onCreate}>프로젝트 생성</Button>}
      </header>

      {projects.length === 0 ? (
        <section className="card empty-state" aria-label="프로젝트 목록">
          <h2>등록된 프로젝트가 없습니다.</h2>
          <p>{user.role === 'QA' ? '첫 프로젝트를 생성해 분석을 시작하세요.' : 'QA가 프로젝트를 생성하면 여기에 표시됩니다.'}</p>
        </section>
      ) : (
        <section className="card project-table-wrap" aria-label="프로젝트 목록">
          <table className="project-table">
            <thead><tr>
              {['프로젝트명', '게임 장르', '플랫폼', '기획서', '생성', '승인', '반려', '미확인 모호성', '최근 분석일'].map((label) => <th key={label}>{label}</th>)}
            </tr></thead>
            <tbody>{projects.map((project) => <tr key={project.id}>
              <td><button className="link-button" onClick={() => onOpen(project.id)}><strong>{project.name}</strong></button>
                {project.description && <small>{project.description}</small>}</td>
              <td>{project.gameGenre || '-'}</td>
              <td>{project.platform || '-'}</td>
              <td>{project.documentCount}</td>
              <td>{project.generatedCount}</td>
              <td>{project.approvedCount}</td>
              <td>{project.rejectedCount}</td>
              <td>{project.openAmbiguityCount ?? '—'}</td>
              <td>{project.lastAnalyzedAt ? new Date(project.lastAnalyzedAt).toLocaleDateString('ko-KR') : '-'}</td>
            </tr>)}</tbody>
          </table>
        </section>
      )}
    </main>
  )
}

export function ProjectCreateForm({
  onSubmit,
  onCancel,
  errorMessage,
}: {
  onSubmit: (values: ProjectValues) => Promise<void>
  onCancel: () => void
  errorMessage?: string
}) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ProjectValues>({
    resolver: zodResolver(projectSchema),
    defaultValues: { name: '', description: '', gameGenre: '', platform: '' },
  })

  return (
    <main className="form-shell">
      <form className="card project-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <div><p className="eyebrow">NEW PROJECT</p><h1>프로젝트 생성</h1></div>
        <label htmlFor="project-name">프로젝트명</label>
        <div className="form-field"><Input id="project-name" autoFocus {...register('name')} />
          {errors.name && <span className="error">{errors.name.message}</span>}
        </div>
        <label htmlFor="project-description">설명</label>
        <textarea id="project-description" rows={4} {...register('description')} />
        <div className="form-grid">
          <div className="form-field"><label htmlFor="project-genre">게임 장르</label><Input id="project-genre" {...register('gameGenre')} /></div>
          <div className="form-field"><label htmlFor="project-platform">플랫폼</label><Input id="project-platform" {...register('platform')} /></div>
        </div>
        {errorMessage && <p className="error" role="alert">{errorMessage}</p>}
        <div className="form-actions">
          <Button type="button" variant="outline" onClick={onCancel}>취소</Button>
          <Button type="submit" disabled={isSubmitting}>{isSubmitting ? '생성 중' : '생성'}</Button>
        </div>
      </form>
    </main>
  )
}

export function DocumentUploadForm({
  onSubmit,
  isSubmitting,
  errorMessage,
}: {
  onSubmit: (values: UploadValues) => Promise<void>
  isSubmitting?: boolean
  errorMessage?: string
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<UploadValues>({
    resolver: zodResolver(uploadSchema),
    defaultValues: { title: '', analyzeAfterUpload: false },
  })
  return <form className="card upload-form" onSubmit={handleSubmit(onSubmit)} noValidate>
    <div><p className="eyebrow">PLANNING DOCUMENT</p><h2>기획서 PDF 업로드</h2></div>
    <div className="form-field"><label htmlFor="document-title">기획서 제목</label>
      <Input id="document-title" {...register('title')} />
      {errors.title && <span className="error">{errors.title.message}</span>}
    </div>
    <div className="form-field"><label htmlFor="document-file">PDF 파일</label>
      <Input id="document-file" type="file" accept="application/pdf,.pdf" {...register('file')} />
      {errors.file && <span className="error">{errors.file.message}</span>}
    </div>
    <label className="checkbox-field"><input type="checkbox" {...register('analyzeAfterUpload')} /> 업로드 후 AI 분석 시작</label>
    {errorMessage && <p className="error" role="alert">{errorMessage}</p>}
    <Button type="submit" disabled={isSubmitting}>{isSubmitting ? '업로드 중' : 'PDF 업로드'}</Button>
  </form>
}

export function ProjectDetail({
  project,
  documents,
  testCases,
  role,
  uploading,
  analyzingDocumentId,
  uploadError,
  analysisError,
  onUpload,
  onAnalyze,
  onOpenDocument,
}: {
  project: ProjectApiItem
  documents: ProjectDocument[]
  testCases: TestCaseItem[]
  role: User['role']
  uploading?: boolean
  analyzingDocumentId?: number
  uploadError?: string
  analysisError?: string
  onUpload: (values: UploadValues) => Promise<void>
  onAnalyze: (documentId: number) => void
  onOpenDocument: (documentId: number) => void
}) {
  const [activeTab, setActiveTab] = useState('테스트 케이스')
  const tabs = ['기능 요약', '요구사항', '테스트 케이스', '모호한 요구사항']
  return <main className="page-shell project-detail-shell">
    <header><div><p className="eyebrow">PROJECT</p><h1>{project.name}</h1>
      <p>{project.gameGenre || '-'} · {project.platform || '-'}</p></div></header>

    {documents.length === 0 ? role === 'QA' ? (
      <DocumentUploadForm onSubmit={onUpload} isSubmitting={uploading} errorMessage={uploadError} />
    ) : (
      <section className="card empty-state"><h2>아직 업로드된 기획서가 없습니다.</h2></section>
    ) : (
      <section className="document-list" aria-label="기획서 목록">
        {documents.map((document) => {
          const documentTestCases = document.analysis
            ? testCases.filter((testCase) => testCase.analysisId === document.analysis?.id)
            : []
          const analyzing = analyzingDocumentId === document.id || ['PENDING', 'PROCESSING'].includes(document.analysis?.status ?? '')
          return <article className="card document-card" key={document.id}>
            <div><button className="link-button" onClick={() => onOpenDocument(document.id)}><h2>{document.title}</h2></button>
              <p>{document.originalFileName} · {document.pageCount}페이지 · {new Date(document.createdAt).toLocaleDateString('ko-KR')}</p></div>
            <span className={`status-badge status-${document.processingStatus.toLowerCase()}`}>{document.processingStatus}</span>
            {document.failureReason && <p className="error">{document.failureReason}</p>}
            {document.processingStatus === 'READY' && <div className="document-actions">
              {documentTestCases.length > 0 ? <a href="#analysis-results">테스트 케이스 보기</a>
                : role === 'QA' && <Button disabled={analyzingDocumentId !== undefined || analyzing}
                    onClick={() => onAnalyze(document.id)}>{analyzing ? 'AI 분석 진행 중' : 'AI 분석 요청'}</Button>}
              {analyzing && <span>분석 결과를 확인하는 중입니다.</span>}
            </div>}
          </article>
        })}
        {analysisError && <p className="error" role="alert">{analysisError}</p>}
      </section>
    )}

    {testCases.length > 0 && <section id="analysis-results" className="card analysis-results">
      <div className="analysis-tabs" role="tablist" aria-label="AI 분석 결과">
        {tabs.map((tab) => <button key={tab} role="tab" aria-selected={activeTab === tab}
          onClick={() => setActiveTab(tab)}>{tab}</button>)}
      </div>
      {activeTab === '테스트 케이스' ? <div className="simple-test-list">
        {testCases.map((testCase) => <article key={testCase.id}>
          <strong>TC {testCase.displayOrder}. {testCase.testItem}</strong>
          <span>{testCase.majorCategory} / {testCase.middleCategory} / {testCase.minorCategory} · {testCase.status}</span>
        </article>)}
      </div> : <p className="tab-placeholder">{activeTab} 탭은 다음 단계에서 제공합니다.</p>}
    </section>}
  </main>
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

export function OutputDownload({
  approvedCount,
  requirementCount,
  role,
  outputs,
  logs,
  creatingType,
  onCreate,
  onDownload,
}: {
  approvedCount: number
  requirementCount: number
  role: User['role']
  outputs: Partial<Record<Output['outputType'], Output>>
  logs: Partial<Record<Output['outputType'], LoopLog[]>>
  creatingType?: Output['outputType']
  onCreate: (type: Output['outputType']) => void
  onDownload: (output: Output) => void
}) {
  return (
    <main className="output-shell">
      <header><p className="eyebrow">OUTPUTS</p><h1>산출물 다운로드</h1></header>
      <section className="output-summary">
        <article className="card metric"><strong>{approvedCount}</strong><span>승인된 테스트 케이스</span></article>
        <article className="card metric"><strong>{requirementCount}</strong><span>포함될 요구사항</span></article>
      </section>

      {(['CSV_EXPORT', 'MARKDOWN_EXPORT'] as const).map((type) => {
        const output = outputs[type]
        const outputLogs = logs[type] ?? []
        const label = type === 'CSV_EXPORT' ? '테스트 케이스 CSV' : '모호 요구사항 Markdown'
        const progress = output?.status === 'SUCCESS' ? 3 : Math.min(outputLogs.length, 3)
        return (
          <section className="card output-card" key={type}>
            <div className="output-heading">
              <div><h2>{label}</h2><p>상태: {output?.status ?? '생성 전'}</p></div>
              <div className="review-actions">
                {role === 'QA' && <Button disabled={creatingType !== undefined}
                  onClick={() => onCreate(type)}>{creatingType === type ? '생성 중' : '생성'}</Button>}
                <Button variant="outline" disabled={output?.status !== 'SUCCESS'}
                  onClick={() => output && onDownload(output)}>{type === 'CSV_EXPORT' ? 'CSV 다운로드' : 'Markdown 다운로드'}</Button>
              </div>
            </div>
            {output && <>
              <label className="loop-progress">생성 진행 상태
                <progress max={3} value={progress} />
              </label>
              {output.failureReason && <p className="error" role="alert">{output.failureReason}</p>}
              <div className="loop-logs" aria-label={`${label} 생성 로그`}>
                {outputLogs.map((log) => <article key={log.id}>
                  <strong>{log.depthStep}회차 · {log.evaluationScore}점</strong>
                  <span>{log.evaluationFeedback}</span>
                </article>)}
              </div>
              <h3>문서 미리보기</h3>
              <pre className="output-preview">{output.finalContent ?? '최종 문서가 아직 생성되지 않았습니다.'}</pre>
            </>}
          </section>
        )
      })}
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

function OutputScreen({ user }: { user: User }) {
  const { projectId } = useParams()
  const id = Number(projectId)
  const [outputs, setOutputs] = useState<Partial<Record<Output['outputType'], Output>>>({})
  const [logs, setLogs] = useState<Partial<Record<Output['outputType'], LoopLog[]>>>({})
  const [creatingType, setCreatingType] = useState<Output['outputType']>()
  const listQuery = useQuery({
    queryKey: ['projects', id, 'test-cases'],
    queryFn: async () => (await api.get<ApiResponse<{ items: TestCaseItem[] }>>(`/api/projects/${id}/test-cases`)).data.data.items,
    enabled: Number.isInteger(id),
  })

  const create = async (type: Output['outputType']) => {
    setCreatingType(type)
    try {
      const path = type === 'CSV_EXPORT' ? 'csv' : 'markdown'
      const output = (await api.post<ApiResponse<Output>>(`/api/projects/${id}/outputs/${path}`)).data.data
      const outputLogs = (await api.get<ApiResponse<LoopLog[]>>(`/api/outputs/${output.id}/loop-logs`)).data.data
      setOutputs((current) => ({ ...current, [type]: output }))
      setLogs((current) => ({ ...current, [type]: outputLogs }))
    } finally {
      setCreatingType(undefined)
    }
  }

  const download = async (output: Output) => {
    const response = await api.get<Blob>(`/api/outputs/${output.id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = output.fileName
    link.click()
    URL.revokeObjectURL(url)
  }

  if (listQuery.isPending) return <main className="loading">산출물 정보 불러오는 중</main>
  if (listQuery.isError) return <main className="loading">산출물 정보를 불러오지 못했습니다.</main>
  const approved = listQuery.data.filter((item) => item.status === 'APPROVED')
  return <OutputDownload approvedCount={approved.length}
    requirementCount={new Set(approved.map((item) => item.requirementId)).size}
    role={user.role} outputs={outputs} logs={logs} creatingType={creatingType}
    onCreate={(type) => { void create(type) }} onDownload={(output) => { void download(output) }} />
}

type ProjectApiItem = Omit<Project, 'documentCount' | 'generatedCount' | 'approvedCount' | 'rejectedCount' | 'openAmbiguityCount' | 'lastAnalyzedAt'>
type AnalysisSummary = { requestedAt: string; completedAt: string | null }

async function loadProjects(): Promise<Project[]> {
  const projects = (await api.get<ApiResponse<ProjectApiItem[]>>('/api/projects')).data.data

  // ponytail: 프로젝트별 병렬 조회는 MVP용이다. 목록 전용 집계 API가 생기면 한 요청으로 교체한다.
  return Promise.all(projects.map(async (project) => {
    const [documents, testCases] = await Promise.all([
      api.get<ApiResponse<PlanningDocument[]>>(`/api/projects/${project.id}/documents`).then((response) => response.data.data),
      api.get<ApiResponse<{ items: TestCaseItem[] }>>(`/api/projects/${project.id}/test-cases`).then((response) => response.data.data.items),
    ])
    const analyses = await Promise.all(documents.map((document) =>
      api.get<ApiResponse<AnalysisSummary>>(`/api/documents/${document.id}/analyses/latest`)
        .then((response) => response.data.data)
        .catch(() => null),
    ))
    const lastAnalyzedAt = analyses.reduce<string | null>((latest, analysis) => {
      const date = analysis?.completedAt ?? analysis?.requestedAt
      return date && (!latest || date > latest) ? date : latest
    }, null)

    return {
      ...project,
      documentCount: documents.length,
      generatedCount: testCases.filter((item) => item.status === 'GENERATED').length,
      approvedCount: testCases.filter((item) => item.status === 'APPROVED').length,
      rejectedCount: testCases.filter((item) => item.status === 'REJECTED').length,
      openAmbiguityCount: null,
      lastAnalyzedAt,
    }
  }))
}

function ProjectScreen({ user }: { user: User }) {
  const navigate = useNavigate()
  const projects = useQuery({ queryKey: ['projects'], queryFn: loadProjects })
  if (projects.isPending) return <main className="loading">프로젝트 불러오는 중</main>
  if (projects.isError) return <main className="loading">프로젝트를 불러오지 못했습니다.</main>
  return <ProjectHome user={user} projects={projects.data} onCreate={() => navigate('/projects/new')}
    onOpen={(projectId) => navigate(`/projects/${projectId}`)} />
}

function ProjectCreateScreen() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const create = useMutation({
    mutationFn: async (values: ProjectValues) => (await api.post<ApiResponse<ProjectApiItem>>('/api/projects', values)).data.data,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects'] })
      navigate('/projects')
    },
  })
  return <ProjectCreateForm
    onSubmit={(values) => create.mutateAsync(values).then(() => undefined)}
    onCancel={() => navigate('/projects')}
    errorMessage={create.isError ? '프로젝트를 생성하지 못했습니다.' : undefined}
  />
}

function ProjectDetailScreen({ user }: { user: User }) {
  const { projectId } = useParams()
  const id = Number(projectId)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [uploadPolling, setUploadPolling] = useState(false)
  const [analyzingDocumentId, setAnalyzingDocumentId] = useState<number>()
  const project = useQuery({
    queryKey: ['projects', id],
    queryFn: async () => (await api.get<ApiResponse<ProjectApiItem>>(`/api/projects/${id}`)).data.data,
    enabled: Number.isInteger(id),
  })
  const documents = useQuery({
    queryKey: ['projects', id, 'documents'],
    queryFn: async () => (await api.get<ApiResponse<PlanningDocument[]>>(`/api/projects/${id}/documents`)).data.data,
    enabled: Number.isInteger(id),
    refetchInterval: (query: { state: { data?: PlanningDocument[] } }) =>
      uploadPolling || query.state.data?.some((document) => ['UPLOADED', 'PROCESSING'].includes(document.processingStatus))
        ? 1500 : false,
  })
  const testCases = useQuery({
    queryKey: ['projects', id, 'test-cases'],
    queryFn: async () => (await api.get<ApiResponse<{ items: TestCaseItem[] }>>(`/api/projects/${id}/test-cases`)).data.data.items,
    enabled: Number.isInteger(id),
  })
  const analyses = useQueries({ queries: (documents.data ?? []).map((document) => ({
    queryKey: ['documents', document.id, 'analyses', 'latest'],
    queryFn: async (): Promise<AnalysisJob | null> => {
      try {
        return (await api.get<ApiResponse<AnalysisJob>>(`/api/documents/${document.id}/analyses/latest`)).data.data
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 404) return null
        throw error
      }
    },
    retry: false,
    refetchInterval: (query: { state: { data?: AnalysisJob | null } }) =>
      analyzingDocumentId === document.id || ['PENDING', 'PROCESSING'].includes(query.state.data?.status ?? '') ? 3000 : false,
  })) })
  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['projects', id, 'documents'] }),
      queryClient.invalidateQueries({ queryKey: ['projects', id, 'test-cases'] }),
      queryClient.invalidateQueries({ queryKey: ['documents'] }),
      queryClient.invalidateQueries({ queryKey: ['projects'] }),
    ])
  }
  const upload = useMutation({
    onMutate: () => setUploadPolling(true),
    mutationFn: async (values: UploadValues) => {
      const form = new FormData()
      form.append('title', values.title)
      form.append('file', values.file[0])
      const document = (await api.post<ApiResponse<PlanningDocument>>(`/api/projects/${id}/documents`, form)).data.data
      if (values.analyzeAfterUpload && document.processingStatus === 'READY') {
        await api.post<ApiResponse<AnalysisJob>>(`/api/documents/${document.id}/analyses`)
      }
    },
    onSettled: async () => {
      setUploadPolling(false)
      await refresh()
    },
  })
  const analysis = useMutation({
    onMutate: (documentId) => setAnalyzingDocumentId(documentId),
    mutationFn: async (documentId: number) =>
      (await api.post<ApiResponse<AnalysisJob>>(`/api/documents/${documentId}/analyses`)).data.data,
    onSettled: async () => {
      setAnalyzingDocumentId(undefined)
      await refresh()
    },
  })

  if (project.isPending || documents.isPending || testCases.isPending) return <main className="loading">프로젝트 상세 불러오는 중</main>
  if (project.isError || documents.isError || testCases.isError || analyses.some((query) => query.isError)) {
    return <main className="loading">프로젝트 상세를 불러오지 못했습니다.</main>
  }
  const projectDocuments = documents.data.map((document, index) => ({
    ...document,
    analysis: analyses[index]?.data ?? null,
  }))
  return <ProjectDetail project={project.data} documents={projectDocuments} testCases={testCases.data}
    role={user.role} uploading={upload.isPending} analyzingDocumentId={analyzingDocumentId}
    uploadError={upload.isError ? '기획서를 업로드하지 못했습니다.' : undefined}
    analysisError={analysis.isError ? 'AI 분석 요청에 실패했습니다.' : undefined}
    onUpload={(values) => upload.mutateAsync(values).then(() => undefined)}
    onAnalyze={(documentId) => analysis.mutate(documentId)}
    onOpenDocument={(documentId) => navigate(`/documents/${documentId}`)} />
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
        element={userQuery.data ? <ProjectScreen user={userQuery.data} /> : <Navigate to="/login" replace />}
      />
      <Route
        path="/projects/new"
        element={userQuery.data?.role === 'QA' ? <ProjectCreateScreen /> : <Navigate to={userQuery.data ? '/projects' : '/login'} replace />}
      />
      <Route
        path="/projects/:projectId"
        element={userQuery.data ? <ProjectDetailScreen user={userQuery.data} /> : <Navigate to="/login" replace />}
      />
      <Route
        path="/documents/:documentId"
        element={userQuery.data ? <DocumentScreen /> : <Navigate to="/login" replace />}
      />
      <Route
        path="/projects/:projectId/test-cases"
        element={userQuery.data ? <TestCaseScreen user={userQuery.data} /> : <Navigate to="/login" replace />}
      />
      <Route
        path="/projects/:projectId/outputs"
        element={userQuery.data ? <OutputScreen user={userQuery.data} /> : <Navigate to="/login" replace />}
      />
      <Route path="*" element={<Navigate to={userQuery.data ? '/projects' : '/login'} replace />} />
    </Routes>
  )
}

export default App
