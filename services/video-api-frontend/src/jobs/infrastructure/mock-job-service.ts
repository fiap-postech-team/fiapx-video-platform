import { DEMO_USER } from '../../auth/infrastructure/mock-authentication-service'
import type {
  CreateJobRequest,
  CreateJobResult,
  Job,
  JobPage,
  JobService,
} from '../domain/job'

export const CONFIRMED_SOURCE_KEY = 'videos/demo/aula-confirmada.mp4'
export const UNCONFIRMED_SOURCE_KEY = 'videos/demo/rascunho.mp4'

const DEFAULT_LIMIT = 20
const MAX_LIMIT = 100

function fingerprint(sourceKey: string): string {
  return sourceKey
}

function seedJobs(now: string): Job[] {
  return [
    {
      id: '3c1a0f6e-7b2d-4c91-9a44-0f8c2d1e9b10',
      userId: DEMO_USER.id,
      sourceKey: 'videos/demo/fonte-pendente.mp4',
      resultKey: null,
      status: 'PENDING',
      createdAt: now,
    },
    {
      id: '8a2b91c4-11d0-4e77-a503-6b9f0c4d2e21',
      userId: DEMO_USER.id,
      sourceKey: 'videos/demo/fonte-processando.mp4',
      resultKey: null,
      status: 'PROCESSING',
      createdAt: '2026-09-18T16:40:00Z',
    },
    {
      id: 'c4e8d217-90ab-4f12-8d33-1a7b5e6c0f32',
      userId: DEMO_USER.id,
      sourceKey: 'videos/demo/fonte-concluida.mp4',
      resultKey: 'videos/demo/resultados/c4e8d217-90ab-4f12-8d33-1a7b5e6c0f32.zip',
      status: 'COMPLETED',
      createdAt: '2026-09-17T11:05:00Z',
    },
    {
      id: 'f0b3a159-2c84-4d60-9e71-4d2a8b7c1e43',
      userId: DEMO_USER.id,
      sourceKey: 'videos/demo/fonte-falha.mp4',
      resultKey: null,
      status: 'FAILED',
      createdAt: '2026-09-16T09:12:00Z',
    },
  ]
}

export class MockJobService implements JobService {
  private readonly jobs: Job[]
  private readonly idempotency = new Map<string, { fingerprint: string; jobId: string }>()

  constructor(clock: () => string = () => '2026-09-19T12:00:00Z') {
    this.jobs = seedJobs(clock())
  }

  async list(userId: string, query?: { cursor?: string; limit?: number }): Promise<JobPage> {
    const limit = query?.limit ?? DEFAULT_LIMIT
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new Error('Invalid page size')
    }

    const owned = this.jobs
      .filter((job) => job.userId === userId)
      .sort((left, right) => right.createdAt.localeCompare(left.createdAt) || right.id.localeCompare(left.id))

    const start = query?.cursor ? owned.findIndex((job) => job.id === query.cursor) + 1 : 0
    const sliceStart = start < 1 && query?.cursor ? owned.length : start
    const items = owned.slice(sliceStart, sliceStart + limit)
    const last = items.at(-1)
    const hasMore = sliceStart + items.length < owned.length

    return {
      items,
      nextCursor: hasMore && last ? last.id : null,
    }
  }

  async get(userId: string, id: string): Promise<Job | null> {
    return this.jobs.find((job) => job.id === id && job.userId === userId) ?? null
  }

  async create(userId: string, request: CreateJobRequest): Promise<CreateJobResult> {
    if (request.idempotencyKey) {
      const recorded = this.idempotency.get(`${userId}:${request.idempotencyKey}`)
      if (recorded) {
        if (recorded.fingerprint !== fingerprint(request.sourceKey)) {
          return {
            error: {
              code: 'IDEMPOTENCY_CONFLICT',
              message: 'A chave de idempotência já foi usada com outro payload.',
            },
          }
        }
        const existing = await this.get(userId, recorded.jobId)
        if (!existing) {
          return { error: { code: 'JOB_UNAVAILABLE', message: 'Não foi possível criar o job. Tente novamente.' } }
        }
        return { job: existing }
      }
    }

    if (request.sourceKey === UNCONFIRMED_SOURCE_KEY) {
      return {
        error: {
          code: 'VIDEO_NOT_CONFIRMED',
          message: 'O vídeo ainda não foi confirmado. A API só cria job para objeto já enviado.',
        },
      }
    }
    if (request.sourceKey !== CONFIRMED_SOURCE_KEY) {
      return {
        error: {
          code: 'VIDEO_NOT_FOUND',
          message: 'Vídeo não encontrado para este proprietário.',
        },
      }
    }

    const job: Job = {
      id: crypto.randomUUID(),
      userId,
      sourceKey: request.sourceKey,
      resultKey: null,
      status: 'PENDING',
      createdAt: new Date().toISOString(),
    }
    this.jobs.unshift(job)
    if (request.idempotencyKey) {
      this.idempotency.set(`${userId}:${request.idempotencyKey}`, {
        fingerprint: fingerprint(request.sourceKey),
        jobId: job.id,
      })
    }
    return { job }
  }
}
