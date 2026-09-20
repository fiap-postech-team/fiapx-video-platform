import { copy } from '../../product-copy'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 128

export function normalizeEmail(email: string): string {
  return email.trim().toLowerCase()
}

export function emailError(email: string): string | undefined {
  if (!email) {
    return copy.access.emailRequired
  }
  if (!EMAIL_PATTERN.test(email)) {
    return copy.access.emailInvalid
  }
  return undefined
}

export function passwordError(password: string): string | undefined {
  if (!password) {
    return copy.access.passwordRequired
  }
  if (password.length < PASSWORD_MIN_LENGTH || password.length > PASSWORD_MAX_LENGTH) {
    return copy.access.passwordLength
  }
  return undefined
}
