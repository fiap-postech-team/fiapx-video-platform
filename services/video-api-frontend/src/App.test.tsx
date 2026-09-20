import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import App from './App'
import type { AuthenticationService } from './auth/domain/authentication'
import { DEMO_CREDENTIALS, DEMO_USER, MockAuthenticationService } from './auth/infrastructure/mock-authentication-service'
import type { JobService } from './jobs/domain/job'
import { CONFIRMED_SOURCE_KEY, MockJobService, UNCONFIRMED_SOURCE_KEY } from './jobs/infrastructure/mock-job-service'

function serviceReturning(
  result: Awaited<ReturnType<AuthenticationService['authenticate']>>,
): AuthenticationService {
  return {
    authenticate: vi.fn().mockResolvedValue(result),
    register: vi.fn(),
    logout: vi.fn().mockResolvedValue(undefined),
  }
}

async function fillValidCredentials(user: ReturnType<typeof userEvent.setup>, email = DEMO_CREDENTIALS.email) {
  await user.type(screen.getByLabelText('E-mail'), email)
  await user.type(screen.getByLabelText('Senha'), DEMO_CREDENTIALS.password)
}

describe('App', () => {
  it('renders the accessible login form', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Entre para acompanhar a extração.' })).toBeInTheDocument()
    expect(screen.getByLabelText('E-mail')).toBeRequired()
    expect(screen.getByLabelText('Senha')).toBeRequired()
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Cadastrar' })).toBeInTheDocument()
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

  it('opens the owner workspace after a successful login', async () => {
    const user = userEvent.setup()
    render(
      <App
        authenticationService={serviceReturning({ user: DEMO_USER })}
        jobService={new MockJobService(() => '2026-09-19T12:00:00Z')}
      />,
    )

    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('heading', { name: 'Meus jobs' })).toBeInTheDocument()
    expect(screen.getByText(DEMO_USER.email)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /videos\/demo\/fonte-pendente\.mp4/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /videos\/demo\/fonte-concluida\.mp4/ })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Novo job' })).toBeInTheDocument()
  })

  it('shows a safe message for rejected credentials', async () => {
    const user = userEvent.setup()
    render(
      <App
        authenticationService={serviceReturning({
          error: { code: 'INVALID_CREDENTIALS', message: 'E-mail ou senha inválidos.' },
        })}
      />,
    )

    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos.')
  })

  it('uses a generic message when authentication is unavailable', async () => {
    const user = userEvent.setup()
    render(
      <App
        authenticationService={serviceReturning({
          error: { code: 'AUTHENTICATION_UNAVAILABLE', message: 'internal details' },
        })}
      />,
    )

    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível concluir o login. Tente novamente.')
  })

  it('prevents duplicate submissions while authentication is pending', async () => {
    let complete: ((value: Awaited<ReturnType<AuthenticationService['authenticate']>>) => void) | undefined
    const authenticationService: AuthenticationService = {
      authenticate: vi.fn().mockImplementation(() => new Promise((resolve) => { complete = resolve })),
      register: vi.fn(),
      logout: vi.fn(),
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
      register: vi.fn(),
      logout: vi.fn(),
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

  it('registers a USER and returns to login without opening the workspace', async () => {
    const user = userEvent.setup()
    const authenticationService = new MockAuthenticationService()
    render(<App authenticationService={authenticationService} jobService={new MockJobService()} />)

    await user.click(screen.getByRole('tab', { name: 'Cadastrar' }))
    await user.type(screen.getByLabelText('E-mail'), 'nova@fiapx.local')
    await user.type(screen.getByLabelText('Senha'), 'SenhaSegura123')
    await user.type(screen.getByLabelText('Confirmar senha'), 'SenhaSegura123')
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Conta criada. Entre com o e-mail e a senha cadastrados.')
    expect(screen.getByRole('heading', { name: 'Entre para acompanhar a extração.' })).toBeInTheDocument()
    expect(screen.getByLabelText('E-mail')).toHaveValue('nova@fiapx.local')
    expect(screen.queryByRole('heading', { name: 'Meus jobs' })).not.toBeInTheDocument()
  })

  it('opens a job detail from the owner list and returns to the composer', async () => {
    const user = userEvent.setup()
    render(
      <App
        authenticationService={serviceReturning({ user: DEMO_USER })}
        jobService={new MockJobService(() => '2026-09-19T12:00:00Z')}
      />,
    )
    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    await screen.findByRole('heading', { name: 'Meus jobs' })

    await user.click(screen.getByRole('button', { name: /PENDING/ }))

    expect(await screen.findByRole('heading', { name: 'Job do proprietário' })).toBeInTheDocument()
    expect(screen.getByRole('article')).toHaveTextContent('videos/demo/fonte-pendente.mp4')

    await user.click(screen.getByRole('button', { name: 'Novo job' }))
    expect(screen.getByRole('heading', { name: 'Novo job' })).toBeInTheDocument()
  })

  it('creates a pending job from a confirmed source key', async () => {
    const user = userEvent.setup()
    render(
      <App
        authenticationService={serviceReturning({ user: DEMO_USER })}
        jobService={new MockJobService()}
      />,
    )
    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    await screen.findByRole('heading', { name: 'Novo job' })

    await user.click(screen.getByRole('button', { name: 'Usar sourceKey confirmada' }))
    await user.click(screen.getByRole('button', { name: 'Criar job' }))

    expect(await screen.findByRole('heading', { name: 'Job do proprietário' })).toBeInTheDocument()
    expect(screen.getAllByText(CONFIRMED_SOURCE_KEY).length).toBeGreaterThan(0)
  })

  it('shows a safe conflict when the source video is not confirmed', async () => {
    const user = userEvent.setup()
    render(
      <App
        authenticationService={serviceReturning({ user: DEMO_USER })}
        jobService={new MockJobService()}
      />,
    )
    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    await screen.findByLabelText('sourceKey')

    await user.type(screen.getByLabelText('sourceKey'), UNCONFIRMED_SOURCE_KEY)
    await user.click(screen.getByRole('button', { name: 'Criar job' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('O vídeo ainda não foi confirmado')
  })

  it('logs out back to the login screen', async () => {
    const user = userEvent.setup()
    const authenticationService = serviceReturning({ user: DEMO_USER })
    render(<App authenticationService={authenticationService} jobService={new MockJobService()} />)

    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    await screen.findByRole('button', { name: 'Sair' })
    await user.click(screen.getByRole('button', { name: 'Sair' }))

    expect(await screen.findByRole('heading', { name: 'Entre para acompanhar a extração.' })).toBeInTheDocument()
    expect(authenticationService.logout).toHaveBeenCalledTimes(1)
  })
})

describe('owner isolation in the workspace', () => {
  it('does not render jobs of another owner', async () => {
    const user = userEvent.setup()
    const jobService: JobService = {
      list: vi.fn().mockResolvedValue({ items: [], nextCursor: null }),
      get: vi.fn(),
      create: vi.fn(),
    }
    render(<App authenticationService={serviceReturning({ user: DEMO_USER })} jobService={jobService} />)

    await fillValidCredentials(user)
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Nenhum job nesta conta. Crie o primeiro a partir de uma sourceKey confirmada.')).toBeInTheDocument()
    expect(jobService.list).toHaveBeenCalledWith(DEMO_USER.id, { cursor: undefined, limit: 20 })
  })
})
