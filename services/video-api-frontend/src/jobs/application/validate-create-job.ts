import type { CreateJobRequest } from '../domain/job'

export const IDEMPOTENCY_KEY_MAX_LENGTH = 128

export type CreateJobField = 'sourceKey' | 'idempotencyKey'
export type CreateJobValidationErrors = Partial<Record<CreateJobField, string>>

export interface CreateJobValidationResult {
  request?: CreateJobRequest
  errors: CreateJobValidationErrors
}

export function validateCreateJob(input: {
  sourceKey: string
  idempotencyKey: string
}): CreateJobValidationResult {
  const sourceKey = input.sourceKey.trim()
  const idempotencyKey = input.idempotencyKey.trim()
  const errors: CreateJobValidationErrors = {}

  if (!sourceKey) {
    errors.sourceKey = 'Informe a sourceKey do vídeo confirmado.'
  }
  if (idempotencyKey.length > IDEMPOTENCY_KEY_MAX_LENGTH) {
    errors.idempotencyKey = `A chave de idempotência deve ter no máximo ${IDEMPOTENCY_KEY_MAX_LENGTH} caracteres.`
  }

  return Object.keys(errors).length > 0
    ? { errors }
    : {
        request: idempotencyKey
          ? { sourceKey, idempotencyKey }
          : { sourceKey },
        errors,
      }
}
