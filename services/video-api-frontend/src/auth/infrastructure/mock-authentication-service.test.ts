import { describe, expect, it } from 'vitest'
import { MockAuthenticationService } from './mock-authentication-service'

describe('MockAuthenticationService', () => {
  const service = new MockAuthenticationService()

  it('authenticates the demonstration user', async () => {
    await expect(
      service.authenticate({ email: 'demo@fiapx.local', password: 'MockPassword123!' }),
    ).resolves.toEqual({
      user: {
        id: 'b7b9ec4d-012e-4b82-8f18-cf90f0d9662b',
        email: 'demo@fiapx.local',
        name: 'Usuário de Demonstração',
      },
    })
  })

  it('returns a typed error for all other credentials', async () => {
    await expect(
      service.authenticate({ email: 'other@fiapx.local', password: 'other' }),
    ).resolves.toEqual({
      error: {
        code: 'INVALID_CREDENTIALS',
        message: 'E-mail ou senha inválidos.',
      },
    })
  })
})
