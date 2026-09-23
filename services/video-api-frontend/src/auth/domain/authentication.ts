export type UserRole = 'USER' | 'ADMIN'

export interface LoginCredentials {
  email: string
  password: string
}

export interface RegisterCredentials extends LoginCredentials {
  confirmPassword: string
}

export interface AuthenticatedUser {
  id: string
  email: string
  roles: UserRole[]
}

export type AuthenticationErrorCode =
  | 'INVALID_CREDENTIALS'
  | 'EMAIL_ALREADY_REGISTERED'
  | 'AUTHENTICATION_UNAVAILABLE'
  | 'SESSION_EXPIRED'
  | 'CSRF_DENIED'
  | 'LOGOUT_FAILED'

export interface AuthenticationError {
  code: AuthenticationErrorCode
  message: string
}

export class AuthenticationFailure extends Error {
  readonly error: AuthenticationError

  constructor(error: AuthenticationError) {
    super(error.message)
    this.name = 'AuthenticationFailure'
    this.error = error
  }
}

export type AuthenticationResult =
  | { user: AuthenticatedUser }
  | { error: AuthenticationError }

export type RegistrationResult =
  | { user: AuthenticatedUser }
  | { error: AuthenticationError }

export type SessionBootstrapResult =
  | { user: AuthenticatedUser }
  | { user: null; error?: AuthenticationError }

export interface AuthenticationService {
  authenticate(credentials: LoginCredentials): Promise<AuthenticationResult>
  register(credentials: LoginCredentials): Promise<RegistrationResult>
  bootstrap(): Promise<SessionBootstrapResult>
  logout(): Promise<void>
  authorizedFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response>
}
