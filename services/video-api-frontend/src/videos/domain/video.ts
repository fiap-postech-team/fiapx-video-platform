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
  jobId: string
  status: ProductProcessingStatus
  requestedAt: string
  startedAt: string | null
  finishedAt: string | null
}

export interface VideoLibraryItem {
  videoRef: string
  originalFilename: string
  status: ProductVideoStatus
  jobId: string | null
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
  activityAt: string
  uploadedAt: string | null
  processing: ProcessingView | null
}

export interface DownloadResult {
  downloadUrl: string
  expiresAt: string
  filename: string
  contentType: string
  sizeBytes: number
}

export interface UploadSelection {
  name: string
  sizeBytes: number
  contentType: string
}

export type UploadPhase = 'sending' | 'confirming' | 'processing'
export type UploadJobStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
export type UploadFailureCode = 'UPLOAD_EXPIRED' | 'UPLOAD_UNCERTAIN' | 'UPLOAD_UNAVAILABLE'

export interface UploadOptions {
  signal?: AbortSignal
  onProgress: (percent: number) => void
  onPhase: (phase: UploadPhase) => void
  onJobStatus: (status: UploadJobStatus) => void
}

export interface UploadResult {
  videoId: string
  jobId: string
  status: UploadJobStatus
}

export class UploadFailure extends Error {
  constructor(readonly code: UploadFailureCode) {
    super(code)
  }
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

export type VideoServiceErrorCode =
  | 'LIST_UNAVAILABLE'
  | 'UPLOAD_UNAVAILABLE'
  | 'DETAIL_UNAVAILABLE'
  | 'DOWNLOAD_NOT_READY'
  | 'DOWNLOAD_INCONSISTENT'
  | 'DOWNLOAD_OBJECT_MISSING'
  | 'DOWNLOAD_STORAGE_UNAVAILABLE'
  | 'DOWNLOAD_NOT_FOUND'

export interface VideoServiceError {
  code: VideoServiceErrorCode
  message: string
}

export function isVideoServiceError(value: unknown): value is VideoServiceError {
  return Boolean(
    value
      && typeof value === 'object'
      && 'code' in value
      && (value.code === 'LIST_UNAVAILABLE'
        || value.code === 'UPLOAD_UNAVAILABLE'
        || value.code === 'DETAIL_UNAVAILABLE'
        || value.code === 'DOWNLOAD_NOT_READY'
        || value.code === 'DOWNLOAD_INCONSISTENT'
        || value.code === 'DOWNLOAD_OBJECT_MISSING'
        || value.code === 'DOWNLOAD_STORAGE_UNAVAILABLE'
        || value.code === 'DOWNLOAD_NOT_FOUND'),
  )
}

export interface VideoService {
  list(page?: number, options?: { scenario?: PrototypeScenario }): Promise<VideoLibraryPage>
  get(videoRef: string): Promise<VideoDetail | null>
  download(jobId: string): Promise<DownloadResult>
  simulateUpload(
    userId: string,
    selection: UploadSelection,
    options?: { scenario?: PrototypeScenario; onProgress?: (percent: number) => void },
  ): Promise<void>
  upload?(file: File, options: UploadOptions): Promise<UploadResult>
}
