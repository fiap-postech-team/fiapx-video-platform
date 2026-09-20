import { describe, expect, it } from 'vitest'
import { validateRegister } from './validate-register'

describe('validateRegister', () => {
  it('rejects missing fields', () => {
    expect(validateRegister({ email: '', password: '', confirmPassword: '' })).toEqual({
      errors: {
        email: 'Informe seu e-mail.',
        password: 'Informe sua senha.',
        confirmPassword: 'Confirme sua senha.',
      },
    })
  })

  it('rejects a password confirmation mismatch', () => {
    expect(
      validateRegister({
        email: 'nova@fiapx.local',
        password: 'MockPassword123!',
        confirmPassword: 'MockPassword124!',
      }),
    ).toEqual({
      errors: { confirmPassword: 'As senhas não coincidem.' },
    })
  })

  it('normalizes email and accepts a valid payload', () => {
    expect(
      validateRegister({
        email: '  Nova@FiapX.local ',
        password: 'MockPassword123!',
        confirmPassword: 'MockPassword123!',
      }),
    ).toEqual({
      credentials: { email: 'nova@fiapx.local', password: 'MockPassword123!' },
      errors: {},
    })
  })
})
