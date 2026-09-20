import type { LoginCredentials } from '../domain/authentication'
import { emailError, normalizeEmail, passwordError } from './credentials'

export type LoginField = 'email' | 'password'
export type LoginValidationErrors = Partial<Record<LoginField, string>>

export interface LoginValidationResult {
  credentials?: LoginCredentials
  errors: LoginValidationErrors
}

export function validateLogin(
  credentials: LoginCredentials,
): LoginValidationResult {
  const email = normalizeEmail(credentials.email)
  const errors: LoginValidationErrors = {}
  const nextEmailError = emailError(email)
  const nextPasswordError = passwordError(credentials.password)

  if (nextEmailError) {
    errors.email = nextEmailError
  }
  if (nextPasswordError) {
    errors.password = nextPasswordError
  }

  return Object.keys(errors).length > 0
    ? { errors }
    : { credentials: { email, password: credentials.password }, errors }
}
