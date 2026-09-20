export const UPLOAD_STATUSES = ['PENDING', 'UPLOADED', 'REJECTED', 'EXPIRED'] as const
export type UploadStatus = (typeof UPLOAD_STATUSES)[number]

export const ATTEMPT_STATUSES = ['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'] as const
export type AttemptStatus = (typeof ATTEMPT_STATUSES)[number]

export interface ProcessingAttempt {
  id: string
  status: AttemptStatus
  createdAt: string
  processedAt: string | null
  availableAt: string | null
}

export interface Video {
  id: string
  ownerId: string
  originalFilename: string
  sizeBytes: number
  contentType: string
  uploadStatus: UploadStatus
  requestedAt: string
  uploadedAt: string | null
  attempts: ProcessingAttempt[]
}

export interface VideoPage {
  items: Video[]
  nextCursor: string | null
}

export interface UploadSelection {
  name: string
  sizeBytes: number
  contentType: string
}

export type ScenarioKind =
  | 'default'
  | 'empty'
  | 'loading'
  | 'list-error'
  | 'upload-error'
  | 'upload-pending'

export interface PrototypeScenario {
  kind: ScenarioKind
  delayMs?: number
}

export type VideoServiceErrorCode = 'LIST_UNAVAILABLE' | 'UPLOAD_UNAVAILABLE'

export interface VideoServiceError {
  code: VideoServiceErrorCode
  message: string
}

export function isVideoServiceError(value: unknown): value is VideoServiceError {
  return Boolean(
    value
      && typeof value === 'object'
      && 'code' in value
      && (value.code === 'LIST_UNAVAILABLE' || value.code === 'UPLOAD_UNAVAILABLE'),
  )
}

export interface VideoService {
  list(
    userId: string,
    query?: { cursor?: string; limit?: number; scenario?: PrototypeScenario },
  ): Promise<VideoPage>
  get(userId: string, videoId: string): Promise<Video | null>
  simulateUpload(
    userId: string,
    selection: UploadSelection,
    options?: { scenario?: PrototypeScenario; onProgress?: (percent: number) => void },
  ): Promise<Video>
}

export type VideoStatusKey = 'pending' | 'uploaded' | 'rejected' | 'expired'
export type ProcessingStatusKey = 'pending' | 'processing' | 'completed' | 'error'
