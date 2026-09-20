const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
export const PASSWORD_MIN_LENGTH = 12
export const PASSWORD_MAX_LENGTH = 128

export function normalizeEmail(email: string): string {
  return email.trim().toLowerCase()
}

export function emailError(email: string): string | undefined {
  if (!email) {
    return 'Informe seu e-mail.'
  }
  if (!EMAIL_PATTERN.test(email)) {
    return 'Informe um e-mail válido.'
  }
  return undefined
}

export function passwordError(password: string): string | undefined {
  if (!password) {
    return 'Informe sua senha.'
  }
  if (password.length < PASSWORD_MIN_LENGTH || password.length > PASSWORD_MAX_LENGTH) {
    return `A senha deve ter entre ${PASSWORD_MIN_LENGTH} e ${PASSWORD_MAX_LENGTH} caracteres.`
  }
  return undefined
}
