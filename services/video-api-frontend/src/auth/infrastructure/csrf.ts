const CSRF_COOKIE = 'XSRF-TOKEN'

export function readCsrfToken(cookieHeader: string): string | null {
  const parts = cookieHeader.split(';')
  for (const part of parts) {
    const trimmed = part.trim()
    if (!trimmed.startsWith(`${CSRF_COOKIE}=`)) {
      continue
    }
    const value = trimmed.slice(CSRF_COOKIE.length + 1)
    if (!value) {
      return null
    }
    try {
      return decodeURIComponent(value)
    } catch {
      return value
    }
  }
  return null
}
