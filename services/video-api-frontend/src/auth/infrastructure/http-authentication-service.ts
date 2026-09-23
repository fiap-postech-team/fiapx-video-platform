import { copy } from '../../product-copy'
import type {
  AuthenticatedUser,
  AuthenticationError,
  AuthenticationResult,
  AuthenticationService,
  LoginCredentials,
  RegistrationResult,
  SessionBootstrapResult,
  UserRole,
} from '../domain/authentication'
import { AuthenticationFailure } from '../domain/authentication'
import { AccessTokenStore } from './access-token-store'
import { videoApiBaseUrl } from './api-base-url'
import { readCsrfToken } from './csrf'

export interface CookieSource {
  cookie: string
}

export interface HttpAuthenticationServiceOptions {
  baseUrl?: string
  fetch?: typeof fetch
  cookies?: CookieSource
  tokens?: AccessTokenStore
}

const ROLES: ReadonlySet<string> = new Set(['USER', 'ADMIN'])

export class HttpAuthenticationService implements AuthenticationService {
  private readonly baseUrl: string
  private readonly fetchImpl: typeof fetch
  private readonly cookies: CookieSource
  private readonly tokens: AccessTokenStore

  constructor(options: HttpAuthenticationServiceOptions = {}) {
    this.baseUrl = videoApiBaseUrl(options.baseUrl)
    this.fetchImpl = options.fetch ?? fetch.bind(globalThis)
    this.cookies = options.cookies ?? (typeof document === 'undefined' ? { cookie: '' } : document)
    this.tokens = options.tokens ?? new AccessTokenStore()
  }

  async register(credentials: LoginCredentials): Promise<RegistrationResult> {
    try {
      const response = await this.fetchImpl(this.url('/v1/auth/register'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials),
      })
      if (response.status === 201) {
        const user = await readUser(response)
        return user ? { user } : { error: unavailable(copy.access.registerUnavailable) }
      }
      if (response.status === 409) {
        return { error: { code: 'EMAIL_ALREADY_REGISTERED', message: copy.access.registerDuplicate } }
      }
      return { error: unavailable(copy.access.registerUnavailable) }
    } catch {
      return { error: unavailable(copy.access.registerUnavailable) }
    }
  }

  async authenticate(credentials: LoginCredentials): Promise<AuthenticationResult> {
    try {
      const response = await this.fetchImpl(this.url('/v1/auth/login'), {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials),
      })
      if (response.status === 401) {
        return { error: { code: 'INVALID_CREDENTIALS', message: copy.access.invalidCredentials } }
      }
      if (!response.ok) {
        return { error: unavailable(copy.access.loginUnavailable) }
      }
      if (!this.storeAccessToken(await readAccessToken(response))) {
        return { error: unavailable(copy.access.loginUnavailable) }
      }
      const user = await this.currentUser()
      return user ? { user } : { error: unavailable(copy.access.loginUnavailable) }
    } catch (error) {
      if (isAuthFailure(error) && error.error.code === 'SESSION_EXPIRED') {
        return { error: unavailable(copy.access.loginUnavailable) }
      }
      return { error: unavailable(copy.access.loginUnavailable) }
    }
  }

  async bootstrap(): Promise<SessionBootstrapResult> {
    if (!this.tokens.get() && !readCsrfToken(this.cookies.cookie)) {
      return { user: null }
    }
    try {
      if (!this.tokens.get()) {
        await this.tokens.refreshOnce(() => this.refreshAccessToken())
      }
      const user = await this.currentUser()
      return user ? { user } : { user: null }
    } catch (error) {
      this.tokens.clear()
      if (isAuthFailure(error) && error.error.code === 'SESSION_EXPIRED') {
        return { user: null }
      }
      if (isAuthFailure(error)) {
        return { user: null, error: error.error }
      }
      return { user: null, error: unavailable(copy.access.sessionCheckUnavailable) }
    }
  }

  async logout(): Promise<void> {
    const response = await this.csrfRequest('/v1/auth/logout')
    if (response.status === 204 || response.status === 401) {
      this.tokens.clear()
      return
    }
    if (response.status === 403) {
      throw new AuthenticationFailure({
        code: 'CSRF_DENIED',
        message: copy.shell.logoutUnavailable,
      })
    }
    throw new AuthenticationFailure({
      code: 'LOGOUT_FAILED',
      message: copy.shell.logoutUnavailable,
    })
  }

  async authorizedFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
    return this.authorizedFetchInternal(input, init, false)
  }

  private async authorizedFetchInternal(
    input: RequestInfo | URL,
    init: RequestInit,
    retried: boolean,
  ): Promise<Response> {
    const url = this.resolve(input)
    if (isRefreshOrLogout(url)) {
      return this.csrfRequest(url)
    }

    const response = await this.fetchImpl(url, this.withBearer(init))
    if (response.status !== 401 || retried) {
      return response
    }

    try {
      await this.tokens.refreshOnce(() => this.refreshAccessToken())
    } catch {
      return response
    }
    return this.authorizedFetchInternal(input, init, true)
  }

  private async currentUser(): Promise<AuthenticatedUser | null> {
    const response = await this.authorizedFetch('/v1/me')
    if (response.status === 401) {
      this.tokens.clear()
      throw new AuthenticationFailure({
        code: 'SESSION_EXPIRED',
        message: copy.access.sessionEnded,
      })
    }
    if (!response.ok) {
      throw new AuthenticationFailure(unavailable(copy.access.loginUnavailable))
    }
    return readUser(response)
  }

  private async refreshAccessToken(): Promise<string> {
    const epoch = this.tokens.currentEpoch()
    const response = await this.csrfRequest('/v1/auth/refresh')
    if (response.status === 401) {
      this.tokens.clear()
      throw new AuthenticationFailure({
        code: 'SESSION_EXPIRED',
        message: copy.access.sessionEnded,
      })
    }
    if (response.status === 403) {
      throw new AuthenticationFailure({
        code: 'CSRF_DENIED',
        message: copy.access.sessionCheckUnavailable,
      })
    }
    if (!response.ok) {
      throw new AuthenticationFailure(unavailable(copy.access.sessionCheckUnavailable))
    }
    const token = await readAccessToken(response)
    if (!token || !this.tokens.setIfEpoch(epoch, token.accessToken, token.expiresIn)) {
      throw new AuthenticationFailure(unavailable(copy.access.sessionCheckUnavailable))
    }
    return token.accessToken
  }

  private async csrfRequest(path: string): Promise<Response> {
    const headers = new Headers()
    const csrf = readCsrfToken(this.cookies.cookie)
    if (csrf) {
      headers.set('X-XSRF-TOKEN', csrf)
    }
    return this.fetchImpl(this.url(path), {
      method: 'POST',
      credentials: 'include',
      headers,
    })
  }

  private withBearer(init: RequestInit): RequestInit {
    const headers = new Headers(init.headers)
    const token = this.tokens.get()
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
    return { ...init, headers }
  }

  private storeAccessToken(token: AccessTokenPayload | null): boolean {
    if (!token) {
      return false
    }
    this.tokens.set(token.accessToken, token.expiresIn)
    return true
  }

  private url(path: string): string {
    return `${this.baseUrl}${path}`
  }

  private resolve(input: RequestInfo | URL): string {
    if (typeof input === 'string' && input.startsWith('http')) {
      return input
    }
    if (input instanceof URL) {
      return input.toString()
    }
    if (typeof input === 'string') {
      return this.url(input.startsWith('/') ? input : `/${input}`)
    }
    return this.url('/v1/me')
  }
}

interface AccessTokenPayload {
  accessToken: string
  expiresIn: number
}

function unavailable(message: string): AuthenticationError {
  return { code: 'AUTHENTICATION_UNAVAILABLE', message }
}

function isAuthFailure(error: unknown): error is AuthenticationFailure {
  return error instanceof AuthenticationFailure
}

function isRefreshOrLogout(url: string): boolean {
  try {
    const path = new URL(url, 'http://local.invalid').pathname
    return path === '/v1/auth/refresh' || path === '/v1/auth/logout'
  } catch {
    return false
  }
}

async function readJson(response: Response): Promise<unknown> {
  try {
    return await response.json()
  } catch {
    return null
  }
}

async function readAccessToken(response: Response): Promise<AccessTokenPayload | null> {
  const body = await readJson(response)
  if (!isRecord(body)) {
    return null
  }
  const accessToken = body.accessToken
  const expiresIn = body.expiresIn
  if (typeof accessToken !== 'string' || accessToken.length === 0) {
    return null
  }
  return {
    accessToken,
    expiresIn: typeof expiresIn === 'number' ? expiresIn : 0,
  }
}

async function readUser(response: Response): Promise<AuthenticatedUser | null> {
  const body = await readJson(response)
  if (!isRecord(body)) {
    return null
  }
  const id = body.id
  const email = body.email
  const roles = Array.isArray(body.roles)
    ? body.roles.filter((role): role is UserRole => typeof role === 'string' && ROLES.has(role))
    : []
  if (typeof id !== 'string' || typeof email !== 'string' || roles.length === 0) {
    return null
  }
  return { id, email, roles }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
