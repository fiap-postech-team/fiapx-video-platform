import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import App from './App'
import type { AuthenticationService } from './auth/domain/authentication'

function serviceReturning(result: Awaited<ReturnType<AuthenticationService['authenticate']>>): AuthenticationService {
  return { authenticate: vi.fn().mockResolvedValue(result) }
}

async function fillValidCredentials(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('E-mail'), 'demo@fiapx.local')
  await user.type(screen.getByLabelText('Senha'), 'MockPassword123!')
}

describe('App', () => {
  it('renders the accessible login form', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Entre para acompanhar a evolução.' })).toBeInTheDocument()
    expect(screen.getByLabelText('E-mail')).toBeRequired()
    expect(screen.getByLabelText('Senha')).toBeRequired()
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('shows field errors and does not call the service when data is invalid', async () => {
    const user = userEvent.setup()
    const authenticationService = serviceReturning({ error: { code: 'INVALID_CREDENTIALS', message: 'unexpected' } })
    render(<App authenticationService={authenticationService} />)

    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(screen.getByText('Informe seu e-mail.')).toHaveAttribute('role', 'alert')
    expect(screen.getByText('Informe sua senha.')).toHaveAttribute('role', 'alert')
    expect(authenticationService.authenticate).not.toHaveBeenCalled()
  })

  it('shows an authenticated session after successful login', async () => {
    const user = userEvent.setup()
    render(<App authenticationService={serviceReturning({ user: { id: 'id', email: 'demo@fiapx.local', name: 'Demo' } })} />)

    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Você está autenticado como Demo.')
  })

  it('shows a safe message for rejected credentials', async () => {
    const user = userEvent.setup()
    render(<App authenticationService={serviceReturning({ error: { code: 'INVALID_CREDENTIALS', message: 'E-mail ou senha inválidos.' } })} />)

    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos.')
  })

  it('uses a generic message when authentication is unavailable', async () => {
    const user = userEvent.setup()
    render(<App authenticationService={serviceReturning({ error: { code: 'AUTHENTICATION_UNAVAILABLE', message: 'internal details' } })} />)

    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível concluir o login. Tente novamente.')
  })

  it('prevents duplicate submissions while authentication is pending', async () => {
    let complete: ((value: Awaited<ReturnType<AuthenticationService['authenticate']>>) => void) | undefined
    const authenticationService: AuthenticationService = {
      authenticate: vi.fn().mockImplementation(() => new Promise((resolve) => { complete = resolve })),
    }
    const user = userEvent.setup()
    render(<App authenticationService={authenticationService} />)

    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(screen.getByRole('button', { name: 'Entrando…' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Entrando…' }))
    expect(authenticationService.authenticate).toHaveBeenCalledTimes(1)

    complete?.({ error: { code: 'INVALID_CREDENTIALS', message: 'E-mail ou senha inválidos.' } })
    expect(await screen.findByText('E-mail ou senha inválidos.')).toBeInTheDocument()
  })

  it('clears an old global error when a new attempt starts', async () => {
    let complete: ((value: Awaited<ReturnType<AuthenticationService['authenticate']>>) => void) | undefined
    const authenticationService: AuthenticationService = {
      authenticate: vi
        .fn()
        .mockResolvedValueOnce({ error: { code: 'INVALID_CREDENTIALS', message: 'E-mail ou senha inválidos.' } })
        .mockImplementationOnce(() => new Promise((resolve) => { complete = resolve })),
    }
    const user = userEvent.setup()
    render(<App authenticationService={authenticationService} />)

    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(await screen.findByText('E-mail ou senha inválidos.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(screen.queryByText('E-mail ou senha inválidos.')).not.toBeInTheDocument()

    complete?.({ error: { code: 'INVALID_CREDENTIALS', message: 'E-mail ou senha inválidos.' } })
    expect(await screen.findByText('E-mail ou senha inválidos.')).toBeInTheDocument()
  })
})
