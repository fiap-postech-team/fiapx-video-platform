import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import App from './App'
import type { AuthenticationService } from './auth/domain/authentication'
import { DEMO_CREDENTIALS, DEMO_USER, MockAuthenticationService } from './auth/infrastructure/mock-authentication-service'
import { copy, findForbiddenTerms } from './product-copy'
import { MockVideoService } from './videos/infrastructure/mock-video-service'

function serviceReturning(
  result: Awaited<ReturnType<AuthenticationService['authenticate']>>,
): AuthenticationService {
  return {
    authenticate: vi.fn().mockResolvedValue(result),
    register: vi.fn(),
    logout: vi.fn().mockResolvedValue(undefined),
  }
}

async function signIn(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(copy.access.emailLabel), DEMO_CREDENTIALS.email)
  await user.type(screen.getByLabelText(copy.access.passwordLabel), DEMO_CREDENTIALS.password)
  await user.click(screen.getByRole('button', { name: copy.access.loginSubmit }))
  await screen.findByRole('heading', { name: copy.videos.title })
}

function assertProductLanguage() {
  expect(findForbiddenTerms(document.body.textContent ?? '')).toEqual([])
}

describe('access', () => {
  it('renders Entrar and Criar conta and keeps field validation local', async () => {
    const user = userEvent.setup()
    const authenticationService = serviceReturning({ error: { code: 'INVALID_CREDENTIALS', message: 'internal' } })
    render(<App authenticationService={authenticationService} />)

    expect(screen.getByRole('heading', { name: copy.access.loginTitle })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: copy.access.createTab })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: copy.access.loginSubmit }))
    expect(screen.getByText(copy.access.emailRequired)).toHaveAttribute('role', 'alert')
    expect(authenticationService.authenticate).not.toHaveBeenCalled()
    assertProductLanguage()
  })

  it('returns to login after a valid registration', async () => {
    const user = userEvent.setup()
    render(<App authenticationService={new MockAuthenticationService()} videoService={new MockVideoService()} />)

    await user.click(screen.getByRole('tab', { name: copy.access.createTab }))
    await user.type(screen.getByLabelText(copy.access.emailLabel), 'nova@fiapx.local')
    await user.type(screen.getByLabelText(copy.access.passwordLabel), 'SenhaSegura123')
    await user.type(screen.getByLabelText(copy.access.confirmPasswordLabel), 'SenhaSegura123')
    await user.click(screen.getByRole('button', { name: copy.access.registerSubmit }))

    expect(await screen.findByRole('status')).toHaveTextContent(copy.access.registerSuccess)
    expect(screen.getByRole('heading', { name: copy.access.loginTitle })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: copy.videos.title })).not.toBeInTheDocument()
  })

  it('shows a safe message for rejected credentials and a generic one when auth is unavailable', async () => {
    const user = userEvent.setup()
    const { rerender } = render(
      <App authenticationService={serviceReturning({ error: { code: 'INVALID_CREDENTIALS', message: 'internal' } })} />,
    )
    await user.type(screen.getByLabelText(copy.access.emailLabel), DEMO_CREDENTIALS.email)
    await user.type(screen.getByLabelText(copy.access.passwordLabel), DEMO_CREDENTIALS.password)
    await user.click(screen.getByRole('button', { name: copy.access.loginSubmit }))
    expect(await screen.findByRole('alert')).toHaveTextContent(copy.access.invalidCredentials)

    rerender(
      <App authenticationService={serviceReturning({ error: { code: 'AUTHENTICATION_UNAVAILABLE', message: 'internal' } })} />,
    )
  })
})

describe('authenticated product', () => {
  it('opens Meus vídeos with one row per file and navigates the shell', async () => {
    const user = userEvent.setup()
    render(
      <App
        authenticationService={serviceReturning({ user: DEMO_USER })}
        videoService={new MockVideoService()}
      />,
    )
    await signIn(user)

    expect(screen.getByText('campanha.mp4')).toBeInTheDocument()
    expect(screen.getAllByText(copy.videoStatus.uploaded).length).toBeGreaterThan(0)
    expect(screen.queryByText(copy.processingStatus.completed)).not.toBeInTheDocument()
    expect(screen.getAllByText('campanha.mp4')).toHaveLength(1)
    expect(screen.getByRole('button', { name: copy.shell.navUpload })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: copy.shell.navProfile })).toBeInTheDocument()
    assertProductLanguage()

    await user.click(screen.getByRole('button', { name: copy.shell.navProfile }))
    expect(screen.getByRole('heading', { name: copy.profile.title })).toBeInTheDocument()
    expect(screen.getAllByText(DEMO_USER.email).length).toBeGreaterThan(0)
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    assertProductLanguage()
  })

  it('opens a dedicated detail with history and a simulated download', async () => {
    const user = userEvent.setup()
    render(
      <App
        authenticationService={serviceReturning({ user: DEMO_USER })}
        videoService={new MockVideoService()}
      />,
    )
    await signIn(user)

    const row = screen.getByText('campanha.mp4').closest('article')
    await user.click(within(row as HTMLElement).getByRole('button', { name: copy.videos.openDetail }))

    expect(await screen.findByRole('heading', { name: 'campanha.mp4' })).toBeInTheDocument()
    expect(screen.getByText(copy.detail.sent)).toBeInTheDocument()
    expect(screen.getByText(copy.detail.history)).toBeInTheDocument()
    expect(screen.getByText(copy.processingStatus.completed)).toBeInTheDocument()
    expect(screen.getByText(copy.processingStatus.error)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: copy.detail.download }))
    expect(screen.getByRole('status')).toHaveTextContent(copy.detail.downloadSimulated)
    assertProductLanguage()
  })

  it('shows an empty list without scenario controls', async () => {
    const user = userEvent.setup()
    render(
      <App
        authenticationService={serviceReturning({ user: DEMO_USER })}
        videoService={new MockVideoService([])}
      />,
    )
    await signIn(user)
    expect(await screen.findByText(copy.videos.emptyBody)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: copy.videos.emptyAction })).toBeInTheDocument()
    expect(screen.queryByText(copy.prototype.title)).not.toBeInTheDocument()
  })

  it('shows a recoverable load error', async () => {
    const user = userEvent.setup()
    render(
      <App
        authenticationService={serviceReturning({ user: DEMO_USER })}
        videoService={{
          list: vi.fn().mockRejectedValue({ code: 'LIST_UNAVAILABLE', message: 'mock' }),
          get: vi.fn(),
          simulateUpload: vi.fn(),
        }}
      />,
    )
    await signIn(user)
    expect(await screen.findByRole('alert')).toHaveTextContent(copy.videos.error)
  })

  it('simulates upload from metadata and never calls the network', async () => {
    const user = userEvent.setup()
    const videoService = new MockVideoService()
    const simulate = vi.spyOn(videoService, 'simulateUpload')
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    render(<App authenticationService={serviceReturning({ user: DEMO_USER })} videoService={videoService} />)
    await signIn(user)

    await user.click(screen.getByRole('button', { name: copy.shell.navUpload }))
    const submitButton = () => screen.getAllByRole('button', { name: copy.upload.submit })
      .find((button) => button.getAttribute('type') === 'submit')
    expect(submitButton()).toBeDisabled()
    const file = new File(['abc'], 'gravacao.webm', { type: 'video/webm' })
    await user.upload(screen.getByLabelText(copy.upload.fileLabel), file)
    expect(screen.getByText(/gravacao\.webm/)).toBeInTheDocument()
    expect(submitButton()).toBeEnabled()
    await user.click(submitButton()!)

    expect(await screen.findByRole('heading', { name: copy.upload.successTitle }, { timeout: 4000 })).toBeInTheDocument()
    expect(screen.getByText(copy.upload.successStatus)).toBeInTheDocument()
    expect(simulate).toHaveBeenCalledWith(
      DEMO_USER.id,
      { name: 'gravacao.webm', sizeBytes: file.size, contentType: 'video/webm' },
      expect.objectContaining({ scenario: { kind: 'default' } }),
    )
    expect(simulate.mock.calls[0]?.[1]).not.toHaveProperty('stream')
    expect(fetchSpy).not.toHaveBeenCalled()
    fetchSpy.mockRestore()
  })

  it('logs out, clears the session and restores the default scenario', async () => {
    const user = userEvent.setup()
    const authenticationService = serviceReturning({ user: DEMO_USER })
    render(<App authenticationService={authenticationService} videoService={new MockVideoService()} />)
    await signIn(user)
    await user.click(screen.getByRole('button', { name: copy.shell.logout }))

    expect(await screen.findByRole('heading', { name: copy.access.loginTitle })).toBeInTheDocument()
    expect(authenticationService.logout).toHaveBeenCalledTimes(1)
  })
})
