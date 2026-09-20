import { type FormEvent, useState } from 'react'
import { IDEMPOTENCY_KEY_MAX_LENGTH, validateCreateJob, type CreateJobValidationErrors } from '../application/validate-create-job'
import type { Job, JobService } from '../domain/job'
import { CONFIRMED_SOURCE_KEY, UNCONFIRMED_SOURCE_KEY } from '../infrastructure/mock-job-service'

interface CreateJobFormProps {
  userId: string
  jobService: JobService
  onCreated: (job: Job) => void
}

const UNAVAILABLE_MESSAGE = 'Não foi possível criar o job. Tente novamente.'

const ERROR_MESSAGES: Record<string, string> = {
  VIDEO_NOT_FOUND: 'Vídeo não encontrado para este proprietário.',
  VIDEO_NOT_CONFIRMED: 'O vídeo ainda não foi confirmado. A API só cria job para objeto já enviado.',
  IDEMPOTENCY_CONFLICT: 'A chave de idempotência já foi usada com outro payload.',
}

export function CreateJobForm({ userId, jobService, onCreated }: CreateJobFormProps) {
  const [sourceKey, setSourceKey] = useState('')
  const [idempotencyKey, setIdempotencyKey] = useState('')
  const [errors, setErrors] = useState<CreateJobValidationErrors>({})
  const [globalError, setGlobalError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return

    setGlobalError(null)
    const validation = validateCreateJob({ sourceKey, idempotencyKey })
    setErrors(validation.errors)
    if (!validation.request) return

    setIsSubmitting(true)
    try {
      const result = await jobService.create(userId, validation.request)
      if ('job' in result) {
        setSourceKey('')
        setIdempotencyKey('')
        setErrors({})
        onCreated(result.job)
        return
      }
      setGlobalError(ERROR_MESSAGES[result.error.code] ?? UNAVAILABLE_MESSAGE)
    } catch {
      setGlobalError(UNAVAILABLE_MESSAGE)
    } finally {
      setIsSubmitting(false)
    }
  }

  function fillConfirmedKey() {
    setSourceKey(CONFIRMED_SOURCE_KEY)
  }

  return (
    <form className="composer" noValidate onSubmit={handleSubmit}>
      <p className="panel-kicker">POST /v1/jobs</p>
      <h2>Novo job</h2>
      <p className="panel-copy">
        A API ainda não expõe upload. O job nasce de uma <code>sourceKey</code> de vídeo já confirmado
        para este proprietário.
      </p>
      {globalError && <p className="alert" role="alert">{globalError}</p>}
      <div className="field">
        <label htmlFor="source-key">sourceKey</label>
        <input
          id="source-key"
          name="sourceKey"
          type="text"
          required
          value={sourceKey}
          onChange={(event) => setSourceKey(event.target.value)}
          aria-invalid={Boolean(errors.sourceKey)}
          aria-describedby={errors.sourceKey ? 'source-key-error' : 'source-key-hint'}
        />
        {errors.sourceKey
          ? <p id="source-key-error" className="field-error" role="alert">{errors.sourceKey}</p>
          : <p id="source-key-hint" className="field-hint">Confirmada nesta demo: {CONFIRMED_SOURCE_KEY}</p>}
      </div>
      <div className="field">
        <label htmlFor="idempotency-key">Idempotency-Key (opcional)</label>
        <input
          id="idempotency-key"
          name="idempotencyKey"
          type="text"
          maxLength={IDEMPOTENCY_KEY_MAX_LENGTH}
          value={idempotencyKey}
          onChange={(event) => setIdempotencyKey(event.target.value)}
          aria-invalid={Boolean(errors.idempotencyKey)}
          aria-describedby={errors.idempotencyKey ? 'idempotency-error' : 'idempotency-hint'}
        />
        {errors.idempotencyKey
          ? <p id="idempotency-error" className="field-error" role="alert">{errors.idempotencyKey}</p>
          : <p id="idempotency-hint" className="field-hint">Até 128 caracteres. A mesma chave com o mesmo payload devolve o job já criado.</p>}
      </div>
      <div className="composer-actions">
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Criando…' : 'Criar job'}
        </button>
        <button type="button" className="ghost" onClick={fillConfirmedKey}>
          Usar sourceKey confirmada
        </button>
      </div>
      <p className="field-hint">
        Para simular 409 de vídeo não confirmado, use <code>{UNCONFIRMED_SOURCE_KEY}</code>.
      </p>
    </form>
  )
}
