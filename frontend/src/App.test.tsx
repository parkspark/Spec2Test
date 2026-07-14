import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { LoginForm, ProjectHome } from './App'

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
})
