import { describe, expect, it } from 'vitest'
import { DEMO_CREDENTIALS, DEMO_USER, MockAuthenticationService } from './mock-authentication-service'

describe('MockAuthenticationService', () => {
  it('authenticates the demonstration user', async () => {
    const service = new MockAuthenticationService()

    await expect(service.authenticate({ ...DEMO_CREDENTIALS })).resolves.toEqual({ user: DEMO_USER })
  })

  it('returns a typed error for rejected credentials', async () => {
    const service = new MockAuthenticationService()

    await expect(
      service.authenticate({ email: 'other@fiapx.local', password: 'MockPassword123!' }),
    ).resolves.toEqual({
      error: {
        code: 'INVALID_CREDENTIALS',
        message: 'E-mail ou senha inválidos.',
      },
    })
  })

  it('registers a USER account and authenticates it afterwards', async () => {
    const service = new MockAuthenticationService()
    const credentials = { email: 'nova@fiapx.local', password: 'SenhaSegura123' }

    const registered = await service.register(credentials)
    expect(registered).toMatchObject({ user: { email: credentials.email, roles: ['USER'] } })
    await expect(service.authenticate(credentials)).resolves.toMatchObject({
      user: { email: credentials.email, roles: ['USER'] },
    })
  })

  it('rejects a duplicate email', async () => {
    const service = new MockAuthenticationService()

    await expect(service.register({ ...DEMO_CREDENTIALS })).resolves.toEqual({
      error: {
        code: 'EMAIL_ALREADY_REGISTERED',
        message: 'Este e-mail já está cadastrado.',
      },
    })
  })
})
