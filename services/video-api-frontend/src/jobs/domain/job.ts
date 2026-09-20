export const JOB_STATUSES = ['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'] as const
export type JobStatus = (typeof JOB_STATUSES)[number]

export interface Job {
  id: string
  userId: string
  sourceKey: string
  resultKey: string | null
  status: JobStatus
  createdAt: string
}

export interface JobPage {
  items: Job[]
  nextCursor: string | null
}

export interface CreateJobRequest {
  sourceKey: string
  idempotencyKey?: string
}

export type CreateJobErrorCode =
  | 'VIDEO_NOT_FOUND'
  | 'VIDEO_NOT_CONFIRMED'
  | 'IDEMPOTENCY_CONFLICT'
  | 'JOB_UNAVAILABLE'

export interface CreateJobError {
  code: CreateJobErrorCode
  message: string
}

export type CreateJobResult = { job: Job } | { error: CreateJobError }

export interface JobService {
  list(userId: string, query?: { cursor?: string; limit?: number }): Promise<JobPage>
  get(userId: string, id: string): Promise<Job | null>
  create(userId: string, request: CreateJobRequest): Promise<CreateJobResult>
}

export const JOB_STATUS_COPY: Record<JobStatus, { label: string; detail: string }> = {
  PENDING: {
    label: 'Na fila',
    detail: 'Job persistido. O processor ainda não publicou o início do processamento.',
  },
  PROCESSING: {
    label: 'Processando',
    detail: 'O processor validou o vídeo e está extraindo frames com FFmpeg.',
  },
  COMPLETED: {
    label: 'Concluído',
    detail: 'O ZIP foi gerado. O resultKey identifica o objeto no storage; o download HTTP ainda não existe na API.',
  },
  FAILED: {
    label: 'Falhou',
    detail: 'Falha terminal. O notification-worker envia o e-mail; o conteúdo do vídeo permanece isolado ao proprietário.',
  },
}
