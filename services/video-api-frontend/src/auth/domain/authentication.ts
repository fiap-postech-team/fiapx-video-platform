export interface LoginCredentials {
  email: string
  password: string
}

export interface AuthenticatedUser {
  id: string
  email: string
  name: string
}

export type AuthenticationErrorCode =
  | 'INVALID_CREDENTIALS'
  | 'AUTHENTICATION_UNAVAILABLE'

export interface AuthenticationError {
  code: AuthenticationErrorCode
  message: string
}

export type AuthenticationResult =
  | { user: AuthenticatedUser }
  | { error: AuthenticationError }

export interface AuthenticationService {
  authenticate(credentials: LoginCredentials): Promise<AuthenticationResult>
}
