import { type FormEvent, useState } from 'react'
import { validateLogin, type LoginValidationErrors } from '../application/validate-login'
import type {
  AuthenticatedUser,
  AuthenticationService,
} from '../domain/authentication'

interface LoginFormProps {
  authenticationService: AuthenticationService
  onAuthenticated: (user: AuthenticatedUser) => void
}

const UNAVAILABLE_MESSAGE = 'Não foi possível concluir o login. Tente novamente.'

export function LoginForm({
  authenticationService,
  onAuthenticated,
}: LoginFormProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<LoginValidationErrors>({})
  const [globalError, setGlobalError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return

    setGlobalError(null)
    const validation = validateLogin({ email, password })
    setErrors(validation.errors)
    if (!validation.credentials) return

    setIsSubmitting(true)
    try {
      const result = await authenticationService.authenticate(validation.credentials)
      if ('user' in result) {
        onAuthenticated(result.user)
        return
      }

      setGlobalError(
        result.error.code === 'INVALID_CREDENTIALS'
          ? result.error.message
          : UNAVAILABLE_MESSAGE,
      )
    } catch {
      setGlobalError(UNAVAILABLE_MESSAGE)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="login-form" noValidate onSubmit={handleSubmit} aria-describedby="login-instructions">
      <p id="login-instructions" className="visually-hidden">Preencha e envie suas credenciais de acesso.</p>
      {globalError && <p className="alert" role="alert">{globalError}</p>}
      <div className="field">
        <label htmlFor="email">E-mail</label>
        <input
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          aria-invalid={Boolean(errors.email)}
          aria-describedby={errors.email ? 'email-error' : undefined}
        />
        {errors.email && <p id="email-error" className="field-error" role="alert">{errors.email}</p>}
      </div>
      <div className="field">
        <label htmlFor="password">Senha</label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          aria-invalid={Boolean(errors.password)}
          aria-describedby={errors.password ? 'password-error' : undefined}
        />
        {errors.password && <p id="password-error" className="field-error" role="alert">{errors.password}</p>}
      </div>
      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Entrando…' : 'Entrar'}
      </button>
    </form>
  )
}
