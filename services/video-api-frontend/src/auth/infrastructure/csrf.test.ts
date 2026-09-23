import { describe, expect, it } from 'vitest'
import { readCsrfToken } from './csrf'

describe('readCsrfToken', () => {
  it('reads the CSRF cookie and ignores other cookies', () => {
    expect(readCsrfToken('theme=dark; XSRF-TOKEN=abc%2Fdef; other=1')).toBe('abc/def')
  })

  it('returns null when the cookie is missing or empty', () => {
    expect(readCsrfToken('theme=dark')).toBeNull()
    expect(readCsrfToken('XSRF-TOKEN=')).toBeNull()
  })
})
