import type { LoginCredentials } from '../domain/authentication'

export type LoginField = 'email' | 'password'
export type LoginValidationErrors = Partial<Record<LoginField, string>>

export interface LoginValidationResult {
  credentials?: LoginCredentials
  errors: LoginValidationErrors
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateLogin(
  credentials: LoginCredentials,
): LoginValidationResult {
  const email = credentials.email.trim()
  const errors: LoginValidationErrors = {}

  if (!email) {
    errors.email = 'Informe seu e-mail.'
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = 'Informe um e-mail válido.'
  }

  if (!credentials.password) {
    errors.password = 'Informe sua senha.'
  }

  return Object.keys(errors).length > 0
    ? { errors }
    : { credentials: { email, password: credentials.password }, errors }
}
