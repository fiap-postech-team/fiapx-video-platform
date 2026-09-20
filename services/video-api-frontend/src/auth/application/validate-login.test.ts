import { describe, expect, it } from 'vitest'
import { validateLogin } from './validate-login'

describe('validateLogin', () => {
  it('rejects a missing email and password', () => {
    expect(validateLogin({ email: '', password: '' })).toEqual({
      errors: { email: 'Informe seu e-mail.', password: 'Informe sua senha.' },
    })
  })

  it('rejects an invalid email', () => {
    expect(validateLogin({ email: 'not-an-email', password: 'MockPassword123!' })).toEqual({
      errors: { email: 'Informe um e-mail válido.' },
    })
  })

  it('rejects a password outside the API length bounds', () => {
    expect(validateLogin({ email: 'demo@fiapx.local', password: 'curta' })).toEqual({
      errors: { password: 'A senha deve ter entre 8 e 128 caracteres.' },
    })
  })

  it('trims and lowercases email without changing the password', () => {
    expect(validateLogin({ email: '  Demo@FiapX.local ', password: ' password ' })).toEqual({
      credentials: { email: 'demo@fiapx.local', password: ' password ' },
      errors: {},
    })
  })
})
