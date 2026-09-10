import type {
  AuthenticationResult,
  AuthenticationService,
  LoginCredentials,
} from '../domain/authentication'

const DEMO_CREDENTIALS = {
  email: 'demo@fiapx.local',
  password: 'MockPassword123!',
} as const

const DEMO_USER = {
  id: 'b7b9ec4d-012e-4b82-8f18-cf90f0d9662b',
  email: DEMO_CREDENTIALS.email,
  name: 'Usuário de Demonstração',
} as const

export class MockAuthenticationService implements AuthenticationService {
  async authenticate(
    credentials: LoginCredentials,
  ): Promise<AuthenticationResult> {
    if (
      credentials.email === DEMO_CREDENTIALS.email &&
      credentials.password === DEMO_CREDENTIALS.password
    ) {
      return { user: DEMO_USER }
    }

    return {
      error: {
        code: 'INVALID_CREDENTIALS',
        message: 'E-mail ou senha inválidos.',
      },
    }
  }
}
