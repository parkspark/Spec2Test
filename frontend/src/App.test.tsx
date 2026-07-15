import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { DocumentViewer, EvidencePanel, LoginForm, OutputDownload, ProjectCreateForm, ProjectDetail, ProjectHome, TestCaseSheet, type Project, type ProjectDocument, type TestCaseItem } from './App'

afterEach(cleanup)

describe('role-based frontend skeleton', () => {
  it('validates the login form before submitting', async () => {
    const onLogin = vi.fn()
    render(<LoginForm onLogin={onLogin} />)

    await userEvent.click(screen.getByRole('button', { name: '???' }))

    expect(await screen.findByText('??? ???? ?????.')).toBeInTheDocument()
    expect(await screen.findByText('????? ?????.')).toBeInTheDocument()
    expect(onLogin).not.toHaveBeenCalled()
  })

  it('shows project creation only to QA', () => {
    const { rerender } = render(
      <ProjectHome user={{ id: 1, email: 'qa@example.com', name: 'QA', role: 'QA' }} />,
    )
    expect(screen.getByRole('button', { name: '프로젝트 생성' })).toBeInTheDocument()

    rerender(<ProjectHome user={{ id: 2, email: 'user@example.com', name: 'User', role: 'USER' }} />)
    expect(screen.queryByRole('button', { name: '프로젝트 생성' })).not.toBeInTheDocument()
  })

  it('renders every project summary column', () => {
    const project: Project = { id: 1, name: '신작 RPG', description: '전투 시스템', gameGenre: 'RPG', platform: 'PC',
      documentCount: 2, generatedCount: 5, approvedCount: 3, rejectedCount: 1, openAmbiguityCount: null,
      lastAnalyzedAt: '2026-07-15T12:00:00' }
    render(<ProjectHome user={{ id: 2, email: 'user@example.com', name: 'User', role: 'USER' }} projects={[project]} />)

    expect(screen.getAllByRole('columnheader')).toHaveLength(9)
    expect(screen.getByText('신작 RPG')).toBeInTheDocument()
    expect(screen.getByText('RPG')).toBeInTheDocument()
    expect(screen.getByText('—')).toBeInTheDocument()
  })

  it('validates and submits the project creation form', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    render(<ProjectCreateForm onSubmit={onSubmit} onCancel={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: '생성' }))
    expect(await screen.findByText('프로젝트명을 입력해 주세요.')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('프로젝트명'), '신작 RPG')
    await userEvent.type(screen.getByLabelText('게임 장르'), 'RPG')
    await userEvent.type(screen.getByLabelText('플랫폼'), 'PC')
    await userEvent.click(screen.getByRole('button', { name: '생성' }))

    expect(onSubmit).toHaveBeenCalledWith({ name: '신작 RPG', description: '', gameGenre: 'RPG', platform: 'PC' }, expect.anything())
  })

  it('shows PDF page text and moves between pages', async () => {
    const onPageChange = vi.fn()
    render(
      <DocumentViewer
        document={{
          id: 4,
          title: '게임 기획서',
          originalFileName: 'game-plan.pdf',
          pageCount: 2,
          processingStatus: 'READY',
          createdBy: 1,
          createdAt: '2026-07-14T16:00:00',
        }}
        page={{
          pageNumber: 1,
          imageUrl: '/api/documents/4/pages/1/image',
          elements: [{
            elementId: 'PAGE-1-TEXT-01',
            elementType: 'TEXT',
            text: '하루 한 번 무료 뽑기를 제공한다.',
            boundingBox: { x: 0.1, y: 0.2, width: 0.5, height: 0.1 },
          }],
        }}
        onPageChange={onPageChange}
      />,
    )

    expect(screen.getByRole('img', { name: '게임 기획서 1 페이지' })).toBeInTheDocument()
    expect(screen.getByText('하루 한 번 무료 뽑기를 제공한다.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: '2 페이지' }))
    expect(onPageChange).toHaveBeenCalledWith(2)
  })
})

const testCase: TestCaseItem = {
  id: 101,
  analysisId: 20,
  requirementId: 30,
  displayOrder: 1,
  majorCategory: '무료 뽑기',
  middleCategory: '보상 지급',
  minorCategory: '일일 무료 횟수',
  testItem: '무료 뽑기 정상 사용',
  status: 'GENERATED',
  preconditions: ['로그인되어 있다.'],
  testSteps: [{ stepNumber: 1, action: '무료 뽑기 버튼을 선택한다.', expectedResult: '보상이 지급된다.' }],
  expectedResults: ['무료 횟수가 0으로 변경된다.'],
  evidenceSummary: { pageNumber: 7, sectionTitle: '무료 뽑기 정책', evidenceType: 'INFERRED', sourceText: '무료 뽑기는 매일 초기화된다.' },
  notes: ['AI 추론 포함', '기획 확인 필요'],
  requiresHumanReview: true,
}

const projectDocument: ProjectDocument = {
  id: 4,
  title: '전투 시스템 기획서',
  originalFileName: 'combat.pdf',
  pageCount: 8,
  processingStatus: 'READY',
  createdBy: 1,
  createdAt: '2026-07-15T12:00:00',
  analysis: null,
}

describe('project detail', () => {
  const project = { id: 1, name: '신작 RPG', description: '전투 시스템', gameGenre: 'RPG', platform: 'PC' }

  it('shows upload only to QA when the project has no documents', async () => {
    const onUpload = vi.fn().mockResolvedValue(undefined)
    const props = { project, documents: [], testCases: [], onUpload, onAnalyze: vi.fn(), onOpenDocument: vi.fn() }
    const { rerender } = render(<ProjectDetail {...props} role="USER" />)
    expect(screen.getByText('아직 업로드된 기획서가 없습니다.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'PDF 업로드' })).not.toBeInTheDocument()

    rerender(<ProjectDetail {...props} role="QA" />)
    await userEvent.type(screen.getByLabelText('기획서 제목'), '전투 기획서')
    await userEvent.upload(screen.getByLabelText('PDF 파일'), new File(['pdf'], 'combat.pdf', { type: 'application/pdf' }))
    await userEvent.click(screen.getByRole('button', { name: 'PDF 업로드' }))
    expect(onUpload).toHaveBeenCalled()
  })

  it('allows QA to request analysis for a ready document', async () => {
    const onAnalyze = vi.fn()
    const { rerender } = render(<ProjectDetail project={project} documents={[projectDocument]} testCases={[]} role="QA"
      onUpload={vi.fn()} onAnalyze={onAnalyze} onOpenDocument={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: 'AI 분석 요청' }))
    expect(onAnalyze).toHaveBeenCalledWith(4)

    rerender(<ProjectDetail project={project} documents={[projectDocument]} testCases={[]} role="QA"
      analyzingDocumentId={4} onUpload={vi.fn()} onAnalyze={onAnalyze} onOpenDocument={vi.fn()} />)
    expect(screen.getByRole('button', { name: 'AI 분석 진행 중' })).toBeDisabled()
    expect(screen.getByText('분석 결과를 확인하는 중입니다.')).toBeInTheDocument()
  })

  it('shows analysis tabs and a simple test case list when results exist', () => {
    render(<ProjectDetail project={project}
      documents={[{ ...projectDocument, analysis: { id: 20, planningDocumentId: 4, status: 'COMPLETED',
        requestedAt: '2026-07-15T12:00:00', completedAt: '2026-07-15T12:01:00' } }]}
      testCases={[testCase]} role="USER" onUpload={vi.fn()} onAnalyze={vi.fn()} onOpenDocument={vi.fn()} />)

    expect(screen.getByRole('link', { name: '테스트 케이스 보기' })).toBeInTheDocument()
    expect(screen.getAllByRole('tab')).toHaveLength(4)
    expect(screen.getByText(/TC 1\. 무료 뽑기 정상 사용/)).toBeInTheDocument()
  })

  it('shows the stored analysis failure reason and allows retry', () => {
    render(<ProjectDetail project={project}
      documents={[{ ...projectDocument, analysis: { id: 21, planningDocumentId: 4, status: 'FAILED',
        requestedAt: '2026-07-15T12:00:00', completedAt: '2026-07-15T12:01:00', failureReason: 'model_not_found' } }]}
      testCases={[]} role="QA" onUpload={vi.fn()} onAnalyze={vi.fn()} onOpenDocument={vi.fn()} />)

    expect(screen.getByRole('alert')).toHaveTextContent('AI 분석 실패: model_not_found')
    expect(screen.getByRole('button', { name: 'AI 분석 요청' })).toBeEnabled()
  })
})

describe('test case sheet', () => {
  it('renders ten columns, evidence summary, and note tags', () => {
    render(<TestCaseSheet items={[testCase]} role="USER" onSelect={vi.fn()} />)

    expect(screen.getAllByRole('columnheader')).toHaveLength(10)
    expect(screen.getByText(/p\.7 \/ 무료 뽑기 정책 \/ INFERRED/)).toBeInTheDocument()
    expect(screen.getByText('AI 추론 포함')).toBeInTheDocument()
    expect(screen.getByText('QA 검토 필수')).toBeInTheDocument()
  })

  it('selects a row and shows the complete detail with QA actions', async () => {
    const onSelect = vi.fn()
    const onApprove = vi.fn().mockResolvedValue(undefined)
    const onReject = vi.fn().mockResolvedValue(undefined)
    const { container } = render(<TestCaseSheet items={[testCase]} selected={testCase} role="QA"
      onSelect={onSelect} onApprove={onApprove} onReject={onReject} />)

    await userEvent.click(container.querySelector('tbody tr')!)
    expect(onSelect).toHaveBeenCalledWith(101)
    expect(within(container).getByRole('region', { name: '선택 테스트 케이스 상세' })).toHaveTextContent('무료 횟수가 0으로 변경된다.')
    await userEvent.click(within(container).getByRole('button', { name: '승인' }))
    expect(screen.getByRole('dialog', { name: '테스트 케이스 승인' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: '승인 확인' }))
    expect(onApprove).toHaveBeenCalledWith(101)

    await userEvent.click(within(container).getByRole('button', { name: '반려' }))
    expect(screen.getByRole('button', { name: '반려 확인' })).toBeDisabled()
    await userEvent.type(screen.getByRole('textbox', { name: '반려 사유 (필수)' }), '근거 부족')
    await userEvent.click(screen.getByRole('button', { name: '반려 확인' }))
    expect(onReject).toHaveBeenCalledWith(101, '근거 부족')
  })

  it('does not expose review actions to USER', () => {
    const { container } = render(<TestCaseSheet items={[testCase]} selected={testCase} role="USER" onSelect={vi.fn()} />)
    expect(within(container).queryByRole('button', { name: '승인' })).not.toBeInTheDocument()
    expect(within(container).queryByRole('button', { name: '반려' })).not.toBeInTheDocument()
  })


  it('selects multiple evidences and highlights coordinates when present', async () => {
    const onSelect = vi.fn()
    render(<EvidencePanel
      document={{ id: 4, title: '게임 기획서', originalFileName: 'game.pdf', pageCount: 7,
        processingStatus: 'READY', createdBy: 1, createdAt: '2026-07-15T00:00:00' }}
      page={{ pageNumber: 7, imageUrl: '/api/documents/4/pages/7/image', elements: [] }}
      evidences={[
        { evidenceType: 'EXPLICIT', verificationStatus: 'EXACT', pageNumber: 7,
          sectionTitle: '무료 뽑기 정책', sourceText: '하루 한 번 무료 뽑기를 제공한다.',
          sourceElementType: 'TEXT', boundingBox: { x: 0.1, y: 0.2, width: 0.5, height: 0.1 }, reason: '횟수가 명시됨' },
        { evidenceType: 'INFERRED', verificationStatus: 'SIMILAR', pageNumber: 6,
          sectionTitle: '보상', sourceText: '보상은 인벤토리에 지급한다.', sourceElementType: 'TEXT', reason: '지급 위치 근거' },
      ]}
      selectedIndex={0}
      onSelect={onSelect}
    />)

    expect(screen.getByText('EXACT')).toBeInTheDocument()
    expect(screen.getByLabelText('근거 영역 하이라이트')).toHaveStyle({ left: '10%', top: '20%' })
    await userEvent.click(screen.getByRole('button', { name: '근거 2 (p.6)' }))
    expect(onSelect).toHaveBeenCalledWith(1)
  })
})

describe('output download', () => {
  it('shows counts, preview, downloads, and loop progress while limiting creation to QA', async () => {
    const onCreate = vi.fn()
    const onDownload = vi.fn()
    const output = { id: 9, outputType: 'CSV_EXPORT' as const, status: 'SUCCESS' as const,
      finalContent: 'No,대분류\n1,무료 뽑기', fileName: 'approved.csv', failureReason: null }
    const { container, rerender } = render(<OutputDownload approvedCount={2} requirementCount={1} role="QA"
      outputs={{ CSV_EXPORT: output }} logs={{ CSV_EXPORT: [{ id: 1, depthStep: 1,
        generatedDraft: 'draft', evaluationScore: 100, evaluationFeedback: '통과', createdAt: '2026-07-15' }] }}
      onCreate={onCreate} onDownload={onDownload} />)

    expect(within(container).getByText('2')).toBeInTheDocument()
    expect(within(container).getByText('1')).toBeInTheDocument()
    expect(within(container).getByText(/No,대분류/)).toBeInTheDocument()
    expect(within(container).getByText('1회차 · 100점')).toBeInTheDocument()
    await userEvent.click(within(container).getAllByRole('button', { name: '생성' })[0])
    expect(onCreate).toHaveBeenCalledWith('CSV_EXPORT')
    await userEvent.click(within(container).getByRole('button', { name: 'CSV 다운로드' }))
    expect(onDownload).toHaveBeenCalledWith(output)

    rerender(<OutputDownload approvedCount={2} requirementCount={1} role="USER"
      outputs={{ CSV_EXPORT: output }} logs={{}} onCreate={onCreate} onDownload={onDownload} />)
    expect(within(container).queryByRole('button', { name: '생성' })).not.toBeInTheDocument()
    expect(within(container).getByRole('button', { name: 'CSV 다운로드' })).toBeEnabled()
  })
})
