import { type FormEvent, useEffect, useState } from 'react'
import { copy } from '../../product-copy'
import { PASSWORD_MAX_LENGTH, PASSWORD_MIN_LENGTH } from '../application/credentials'
import { validateLogin, type LoginValidationErrors } from '../application/validate-login'
import type {
  AuthenticatedUser,
  AuthenticationService,
} from '../domain/authentication'

interface LoginFormProps {
  authenticationService: AuthenticationService
  onAuthenticated: (user: AuthenticatedUser) => void
  initialEmail?: string
}

export function LoginForm({
  authenticationService,
  onAuthenticated,
  initialEmail = '',
}: LoginFormProps) {
  const [email, setEmail] = useState(initialEmail)
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<LoginValidationErrors>({})
  const [globalError, setGlobalError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    setEmail(initialEmail)
  }, [initialEmail])

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
          ? copy.access.invalidCredentials
          : copy.access.loginUnavailable,
      )
    } catch {
      setGlobalError(copy.access.loginUnavailable)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="access-form" noValidate onSubmit={handleSubmit} aria-describedby="login-instructions">
      <p id="login-instructions" className="visually-hidden">{copy.access.loginInstructions}</p>
      {globalError && <p className="alert" role="alert">{globalError}</p>}
      <div className="field">
        <label htmlFor="email">{copy.access.emailLabel}</label>
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
        <label htmlFor="password">{copy.access.passwordLabel}</label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          minLength={PASSWORD_MIN_LENGTH}
          maxLength={PASSWORD_MAX_LENGTH}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          aria-invalid={Boolean(errors.password)}
          aria-describedby={errors.password ? 'password-error' : undefined}
        />
        {errors.password && <p id="password-error" className="field-error" role="alert">{errors.password}</p>}
      </div>
      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? copy.access.loginPending : copy.access.loginSubmit}
      </button>
    </form>
  )
}
