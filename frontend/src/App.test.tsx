import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { DocumentViewer, LoginForm, ProjectHome } from './App'

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
