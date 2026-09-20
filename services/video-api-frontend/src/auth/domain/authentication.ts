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

export interface AuthenticationError {
  code: AuthenticationErrorCode
  message: string
}

export type AuthenticationResult =
  | { user: AuthenticatedUser }
  | { error: AuthenticationError }

export type RegistrationResult =
  | { user: Omit<AuthenticatedUser, 'roles'> & { roles: UserRole[] } }
  | { error: AuthenticationError }

export interface AuthenticationService {
  authenticate(credentials: LoginCredentials): Promise<AuthenticationResult>
  register(credentials: LoginCredentials): Promise<RegistrationResult>
  logout(): Promise<void>
}
