import { copy } from '../../product-copy'
import type { LoginCredentials, RegisterCredentials } from '../domain/authentication'
import { emailError, normalizeEmail, passwordError } from './credentials'

export type RegisterField = 'email' | 'password' | 'confirmPassword'
export type RegisterValidationErrors = Partial<Record<RegisterField, string>>

export interface RegisterValidationResult {
  credentials?: LoginCredentials
  errors: RegisterValidationErrors
}

export function validateRegister(
  credentials: RegisterCredentials,
): RegisterValidationResult {
  const email = normalizeEmail(credentials.email)
  const errors: RegisterValidationErrors = {}
  const nextEmailError = emailError(email)
  const nextPasswordError = passwordError(credentials.password)

  if (nextEmailError) {
    errors.email = nextEmailError
  }
  if (nextPasswordError) {
    errors.password = nextPasswordError
  }
  if (!credentials.confirmPassword) {
    errors.confirmPassword = copy.access.confirmRequired
  } else if (credentials.confirmPassword !== credentials.password) {
    errors.confirmPassword = copy.access.passwordMismatch
  }

  return Object.keys(errors).length > 0
    ? { errors }
    : { credentials: { email, password: credentials.password }, errors }
}
