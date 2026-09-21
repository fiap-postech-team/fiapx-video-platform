export const PRODUCT_VIDEO_STATUSES = [
  'AWAITING_UPLOAD',
  'UPLOADED',
  'PROCESSING',
  'AVAILABLE',
  'FAILED',
  'REJECTED',
  'EXPIRED',
] as const
export type ProductVideoStatus = (typeof PRODUCT_VIDEO_STATUSES)[number]

export const PRODUCT_PROCESSING_STATUSES = ['QUEUED', 'PROCESSING', 'AVAILABLE', 'FAILED'] as const
export type ProductProcessingStatus = (typeof PRODUCT_PROCESSING_STATUSES)[number]

export const LIFECYCLE_TONES = [
  'pending',
  'processing',
  'available',
  'rejected',
  'expired',
  'failed',
] as const
export type LifecycleTone = (typeof LIFECYCLE_TONES)[number]

export interface ProcessingView {
  status: ProductProcessingStatus
  requestedAt: string
  startedAt: string | null
  finishedAt: string | null
}

export interface VideoLibraryItem {
  videoRef: string
  originalFilename: string
  status: ProductVideoStatus
  submittedAt: string
  activityAt: string
}

export interface VideoLibraryPage {
  items: VideoLibraryItem[]
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export interface VideoDetail {
  videoRef: string
  originalFilename: string
  status: ProductVideoStatus
  submittedAt: string
  uploadedAt: string | null
  processing: ProcessingView | null
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

export type VideoServiceErrorCode = 'LIST_UNAVAILABLE' | 'UPLOAD_UNAVAILABLE' | 'DETAIL_UNAVAILABLE'

export interface VideoServiceError {
  code: VideoServiceErrorCode
  message: string
}

export function isVideoServiceError(value: unknown): boolean {
  return Boolean(
    value
      && typeof value === 'object'
      && 'code' in value
      && (value.code === 'LIST_UNAVAILABLE'
        || value.code === 'UPLOAD_UNAVAILABLE'
        || value.code === 'DETAIL_UNAVAILABLE'),
  )
}

export interface VideoService {
  list(page?: number, options?: { scenario?: PrototypeScenario }): Promise<VideoLibraryPage>
  get(videoRef: string): Promise<VideoDetail | null>
  simulateUpload(
    userId: string,
    selection: UploadSelection,
    options?: { scenario?: PrototypeScenario; onProgress?: (percent: number) => void },
  ): Promise<void>
}
