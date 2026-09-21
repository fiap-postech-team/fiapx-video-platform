import { describe, expect, it, vi } from 'vitest'
import { copy } from '../../product-copy'
import { AuthenticationFailure } from '../domain/authentication'
import { AccessTokenStore } from './access-token-store'
import { HttpAuthenticationService } from './http-authentication-service'

const USER = {
  id: 'b7b9ec4d-012e-4b82-8f18-cf90f0d9662b',
  email: 'pessoa@exemplo.com',
  roles: ['USER'],
}

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function empty(status: number): Response {
  return new Response(null, { status })
}

describe('HttpAuthenticationService', () => {
  it('registers without creating a session and maps a duplicate email', async () => {
    const fetchImpl = vi.fn()
      .mockResolvedValueOnce(json(201, USER))
      .mockResolvedValueOnce(json(409, { code: 'EMAIL_ALREADY_REGISTERED' }))
    const service = new HttpAuthenticationService({ fetch: fetchImpl, cookies: { cookie: '' } })

    await expect(service.register({ email: USER.email, password: 'SenhaSegura123' }))
      .resolves.toEqual({ user: USER })
    expect(fetchImpl).toHaveBeenCalledWith(
      'http://localhost:8080/v1/auth/register',
      expect.objectContaining({ method: 'POST' }),
    )
    expect(await service.bootstrap()).toEqual({ user: null })

    await expect(service.register({ email: USER.email, password: 'SenhaSegura123' }))
      .resolves.toEqual({
        error: { code: 'EMAIL_ALREADY_REGISTERED', message: copy.access.registerDuplicate },
      })
  })

  it('stores the access token only in memory and loads the account from /v1/me', async () => {
    const tokens = new AccessTokenStore()
    const fetchImpl = vi.fn()
      .mockResolvedValueOnce(json(200, { accessToken: 'access-1', tokenType: 'Bearer', expiresIn: 900, user: USER }))
      .mockResolvedValueOnce(json(200, USER))
    const setItem = vi.spyOn(Storage.prototype, 'setItem')
    const service = new HttpAuthenticationService({
      fetch: fetchImpl,
      cookies: { cookie: 'XSRF-TOKEN=csrf' },
      tokens,
    })

    await expect(service.authenticate({ email: USER.email, password: 'SenhaSegura123' }))
      .resolves.toEqual({ user: USER })

    expect(tokens.get()).toBe('access-1')
    expect(setItem).not.toHaveBeenCalled()
    expect(fetchImpl.mock.calls[1]?.[0]).toBe('http://localhost:8080/v1/me')
    expect(String(new Headers(fetchImpl.mock.calls[1]?.[1].headers).get('Authorization'))).toBe('Bearer access-1')
    setItem.mockRestore()
  })

  it('maps rejected credentials without revealing whether the email exists', async () => {
    const service = new HttpAuthenticationService({
      fetch: vi.fn().mockResolvedValue(json(401, { code: 'AUTHENTICATION_FAILED' })),
      cookies: { cookie: '' },
    })

    await expect(service.authenticate({ email: USER.email, password: 'errada' }))
      .resolves.toEqual({
        error: { code: 'INVALID_CREDENTIALS', message: copy.access.invalidCredentials },
      })
  })

  it('restores a session from refresh when a CSRF cookie is present', async () => {
    const fetchImpl = vi.fn()
      .mockResolvedValueOnce(json(200, { accessToken: 'access-2', tokenType: 'Bearer', expiresIn: 900 }))
      .mockResolvedValueOnce(json(200, USER))
    const service = new HttpAuthenticationService({
      fetch: fetchImpl,
      cookies: { cookie: 'XSRF-TOKEN=csrf-token' },
    })

    await expect(service.bootstrap()).resolves.toEqual({ user: USER })
    expect(fetchImpl.mock.calls[0]?.[0]).toBe('http://localhost:8080/v1/auth/refresh')
    expect(new Headers(fetchImpl.mock.calls[0]?.[1].headers).get('X-XSRF-TOKEN')).toBe('csrf-token')
    expect(fetchImpl.mock.calls[0]?.[1].credentials).toBe('include')
  })

  it('skips refresh when there is no CSRF cookie', async () => {
    const fetchImpl = vi.fn()
    const service = new HttpAuthenticationService({ fetch: fetchImpl, cookies: { cookie: '' } })

    await expect(service.bootstrap()).resolves.toEqual({ user: null })
    expect(fetchImpl).not.toHaveBeenCalled()
  })

  it('reuses an in-memory token and shares a single refresh during bootstrap', async () => {
    const tokens = new AccessTokenStore()
    tokens.set('access-memory', 900)
    const fetchImpl = vi.fn().mockResolvedValue(json(200, USER))
    const withToken = new HttpAuthenticationService({
      fetch: fetchImpl,
      cookies: { cookie: 'XSRF-TOKEN=csrf' },
      tokens,
    })
    await expect(withToken.bootstrap()).resolves.toEqual({ user: USER })
    expect(fetchImpl.mock.calls.map((call) => call[0])).toEqual(['http://localhost:8080/v1/me'])

    let release!: () => void
    const gate = new Promise<void>((resolve) => {
      release = resolve
    })
    const concurrentFetch = vi.fn(async (url: string) => {
      if (String(url).endsWith('/v1/auth/refresh')) {
        await gate
        return json(200, { accessToken: 'access-shared', tokenType: 'Bearer', expiresIn: 900 })
      }
      return json(200, USER)
    })
    const concurrent = new HttpAuthenticationService({
      fetch: concurrentFetch as typeof fetch,
      cookies: { cookie: 'XSRF-TOKEN=csrf' },
      tokens: new AccessTokenStore(),
    })
    const first = concurrent.bootstrap()
    const second = concurrent.bootstrap()
    await Promise.resolve()
    expect(concurrentFetch.mock.calls.filter((call) => String(call[0]).endsWith('/v1/auth/refresh'))).toHaveLength(1)
    release()
    await expect(Promise.all([first, second])).resolves.toEqual([{ user: USER }, { user: USER }])
  })

  it('shares one refresh among concurrent 401s and retries each request once', async () => {
    const fetchImpl = vi.fn()
      .mockResolvedValueOnce(empty(401))
      .mockResolvedValueOnce(empty(401))
      .mockResolvedValueOnce(json(200, { accessToken: 'access-3', tokenType: 'Bearer', expiresIn: 900 }))
      .mockResolvedValueOnce(json(200, { id: 'ok' }))
      .mockResolvedValueOnce(json(200, { id: 'ok' }))
    const tokens = new AccessTokenStore()
    tokens.set('expired', 1)
    const service = new HttpAuthenticationService({
      fetch: fetchImpl,
      cookies: { cookie: 'XSRF-TOKEN=csrf' },
      tokens,
    })

    const [first, second] = await Promise.all([
      service.authorizedFetch('/v1/jobs'),
      service.authorizedFetch('/v1/jobs'),
    ])

    expect(first.status).toBe(200)
    expect(second.status).toBe(200)
    const refreshCalls = fetchImpl.mock.calls.filter((call) => String(call[0]).endsWith('/v1/auth/refresh'))
    expect(refreshCalls).toHaveLength(1)
    expect(tokens.get()).toBe('access-3')
  })

  it('sends CSRF only for refresh and logout', async () => {
    const fetchImpl = vi.fn()
      .mockResolvedValueOnce(json(200, USER))
      .mockResolvedValueOnce(json(200, { accessToken: 'access-4', tokenType: 'Bearer', expiresIn: 900 }))
      .mockResolvedValueOnce(empty(204))
    const tokens = new AccessTokenStore()
    tokens.set('access-0', 900)
    const service = new HttpAuthenticationService({
      fetch: fetchImpl,
      cookies: { cookie: 'XSRF-TOKEN=csrf' },
      tokens,
    })

    await service.authorizedFetch('/v1/me')
    await service.authorizedFetch('/v1/auth/refresh')
    await service.logout()

    expect(new Headers(fetchImpl.mock.calls[0]?.[1].headers).get('X-XSRF-TOKEN')).toBeNull()
    expect(new Headers(fetchImpl.mock.calls[1]?.[1].headers).get('X-XSRF-TOKEN')).toBe('csrf')
    expect(new Headers(fetchImpl.mock.calls[2]?.[1].headers).get('X-XSRF-TOKEN')).toBe('csrf')
    expect(tokens.get()).toBeNull()
  })

  it('does not clear the session when logout is not confirmed', async () => {
    const tokens = new AccessTokenStore()
    tokens.set('access-5', 900)
    const service = new HttpAuthenticationService({
      fetch: vi.fn().mockResolvedValue(empty(503)),
      cookies: { cookie: 'XSRF-TOKEN=csrf' },
      tokens,
    })

    await expect(service.logout()).rejects.toBeInstanceOf(AuthenticationFailure)
    expect(tokens.get()).toBe('access-5')
  })
})
