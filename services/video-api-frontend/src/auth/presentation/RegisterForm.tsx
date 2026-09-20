import { type FormEvent, useState } from 'react'
import { PASSWORD_MAX_LENGTH, PASSWORD_MIN_LENGTH } from '../application/credentials'
import { validateRegister, type RegisterValidationErrors } from '../application/validate-register'
import type { AuthenticationService } from '../domain/authentication'

interface RegisterFormProps {
  authenticationService: AuthenticationService
  onRegistered: (email: string) => void
}

const UNAVAILABLE_MESSAGE = 'Não foi possível concluir o cadastro. Tente novamente.'

export function RegisterForm({ authenticationService, onRegistered }: RegisterFormProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errors, setErrors] = useState<RegisterValidationErrors>({})
  const [globalError, setGlobalError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return

    setGlobalError(null)
    const validation = validateRegister({ email, password, confirmPassword })
    setErrors(validation.errors)
    if (!validation.credentials) return

    setIsSubmitting(true)
    try {
      const result = await authenticationService.register(validation.credentials)
      if ('user' in result) {
        onRegistered(result.user.email)
        return
      }

      setGlobalError(
        result.error.code === 'EMAIL_ALREADY_REGISTERED'
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
    <form className="access-form" noValidate onSubmit={handleSubmit} aria-describedby="register-instructions">
      <p id="register-instructions" className="visually-hidden">
        Cadastre uma conta USER. A senha deve ter entre {PASSWORD_MIN_LENGTH} e {PASSWORD_MAX_LENGTH} caracteres.
      </p>
      {globalError && <p className="alert" role="alert">{globalError}</p>}
      <div className="field">
        <label htmlFor="register-email">E-mail</label>
        <input
          id="register-email"
          name="email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          aria-invalid={Boolean(errors.email)}
          aria-describedby={errors.email ? 'register-email-error' : undefined}
        />
        {errors.email && <p id="register-email-error" className="field-error" role="alert">{errors.email}</p>}
      </div>
      <div className="field">
        <label htmlFor="register-password">Senha</label>
        <input
          id="register-password"
          name="password"
          type="password"
          autoComplete="new-password"
          required
          minLength={PASSWORD_MIN_LENGTH}
          maxLength={PASSWORD_MAX_LENGTH}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          aria-invalid={Boolean(errors.password)}
          aria-describedby={errors.password ? 'register-password-error' : undefined}
        />
        {errors.password && <p id="register-password-error" className="field-error" role="alert">{errors.password}</p>}
      </div>
      <div className="field">
        <label htmlFor="register-confirm-password">Confirmar senha</label>
        <input
          id="register-confirm-password"
          name="confirmPassword"
          type="password"
          autoComplete="new-password"
          required
          minLength={PASSWORD_MIN_LENGTH}
          maxLength={PASSWORD_MAX_LENGTH}
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          aria-invalid={Boolean(errors.confirmPassword)}
          aria-describedby={errors.confirmPassword ? 'register-confirm-error' : undefined}
        />
        {errors.confirmPassword && (
          <p id="register-confirm-error" className="field-error" role="alert">{errors.confirmPassword}</p>
        )}
      </div>
      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Cadastrando…' : 'Criar conta'}
      </button>
    </form>
  )
}
