import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { DocumentViewer, EvidencePanel, LoginForm, ProjectHome, TestCaseSheet, type TestCaseItem } from './App'

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
    expect(screen.getByRole('button', { name: '? ????' })).toBeInTheDocument()

    rerender(<ProjectHome user={{ id: 2, email: 'user@example.com', name: 'User', role: 'USER' }} />)
    expect(screen.queryByRole('button', { name: '? ????' })).not.toBeInTheDocument()
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
