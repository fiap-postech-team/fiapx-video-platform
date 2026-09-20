import { describe, expect, it } from 'vitest'
import { validateCreateJob } from './validate-create-job'

describe('validateCreateJob', () => {
  it('rejects a blank sourceKey', () => {
    expect(validateCreateJob({ sourceKey: '  ', idempotencyKey: '' })).toEqual({
      errors: { sourceKey: 'Informe a sourceKey do vídeo confirmado.' },
    })
  })

  it('rejects an idempotency key longer than 128 characters', () => {
    expect(validateCreateJob({ sourceKey: 'videos/demo/aula-confirmada.mp4', idempotencyKey: 'k'.repeat(129) })).toEqual({
      errors: { idempotencyKey: 'A chave de idempotência deve ter no máximo 128 caracteres.' },
    })
  })

  it('trims fields and omits an empty idempotency key', () => {
    expect(validateCreateJob({ sourceKey: '  videos/demo/aula-confirmada.mp4  ', idempotencyKey: '  ' })).toEqual({
      request: { sourceKey: 'videos/demo/aula-confirmada.mp4' },
      errors: {},
    })
  })
})
