import { describe, expect, it } from 'vitest'
import { validateLogin } from './validate-login'

describe('validateLogin', () => {
  it('rejects a missing email and password', () => {
    expect(validateLogin({ email: '', password: '' })).toEqual({
      errors: { email: 'Informe seu e-mail.', password: 'Informe sua senha.' },
    })
  })

  it('rejects an invalid email', () => {
    expect(validateLogin({ email: 'not-an-email', password: 'password' })).toEqual({
      errors: { email: 'Informe um e-mail válido.' },
    })
  })

  it('trims email without changing the password', () => {
    expect(validateLogin({ email: '  demo@fiapx.local ', password: ' password ' })).toEqual({
      credentials: { email: 'demo@fiapx.local', password: ' password ' },
      errors: {},
    })
  })
})
