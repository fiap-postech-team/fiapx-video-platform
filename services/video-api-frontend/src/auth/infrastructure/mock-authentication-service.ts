import type {
  AuthenticatedUser,
  AuthenticationResult,
  AuthenticationService,
  LoginCredentials,
  RegistrationResult,
} from '../domain/authentication'

export const DEMO_CREDENTIALS = {
  email: 'demo@fiapx.local',
  password: 'MockPassword123!',
} as const

export const DEMO_USER: AuthenticatedUser = {
  id: 'b7b9ec4d-012e-4b82-8f18-cf90f0d9662b',
  email: DEMO_CREDENTIALS.email,
  roles: ['USER'],
}

const INVALID_CREDENTIALS = {
  code: 'INVALID_CREDENTIALS' as const,
  message: 'E-mail ou senha inválidos.',
}

const MAX_FAILURES = 5

interface StoredAccount {
  user: AuthenticatedUser
  password: string
  failedAttempts: number
}

export class MockAuthenticationService implements AuthenticationService {
  private readonly accounts = new Map<string, StoredAccount>([
    [
      DEMO_USER.email,
      { user: DEMO_USER, password: DEMO_CREDENTIALS.password, failedAttempts: 0 },
    ],
  ])

  async authenticate(credentials: LoginCredentials): Promise<AuthenticationResult> {
    const account = this.accounts.get(credentials.email)
    if (!account || account.password !== credentials.password || account.failedAttempts >= MAX_FAILURES) {
      if (account && account.password !== credentials.password) {
        account.failedAttempts += 1
      }
      return { error: INVALID_CREDENTIALS }
    }

    account.failedAttempts = 0
    return { user: account.user }
  }

  async register(credentials: LoginCredentials): Promise<RegistrationResult> {
    if (this.accounts.has(credentials.email)) {
      return {
        error: {
          code: 'EMAIL_ALREADY_REGISTERED',
          message: 'Este e-mail já está cadastrado.',
        },
      }
    }

    const user: AuthenticatedUser = {
      id: crypto.randomUUID(),
      email: credentials.email,
      roles: ['USER'],
    }
    this.accounts.set(user.email, { user, password: credentials.password, failedAttempts: 0 })
    return { user }
  }

  async logout(): Promise<void> {
    return undefined
  }
}
