import { videoApiBaseUrl } from '../../auth/infrastructure/api-base-url'
import {
  PRODUCT_PROCESSING_STATUSES,
  PRODUCT_VIDEO_STATUSES,
  type ProductProcessingStatus,
  type ProductVideoStatus,
  type VideoDetail,
  type VideoLibraryItem,
  type VideoLibraryPage,
  type VideoService,
} from '../domain/video'

export type AuthorizedFetch = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>

export class VideoHttpService implements VideoService {
  private readonly baseUrl: string
  private readonly authorizedFetch: AuthorizedFetch

  constructor(authorizedFetch: AuthorizedFetch, baseUrl?: string) {
    this.authorizedFetch = authorizedFetch
    this.baseUrl = videoApiBaseUrl(baseUrl)
  }

  async list(page = 1): Promise<VideoLibraryPage> {
    const response = await this.authorizedFetch(`${this.baseUrl}/v1/videos?page=${page}`)
    if (!response.ok) {
      throw { code: 'LIST_UNAVAILABLE', message: 'list unavailable' }
    }
    const body: unknown = await response.json()
    const pageBody = asRecord(body)
    const items = asArray(pageBody.items).map(readItem)
    return {
      items,
      page: asNumber(pageBody.page, page),
      pageSize: asNumber(pageBody.pageSize, 5),
      totalItems: asNumber(pageBody.totalItems, items.length),
      totalPages: asNumber(pageBody.totalPages, 0),
    }
  }

  async get(videoRef: string): Promise<VideoDetail | null> {
    const response = await this.authorizedFetch(`${this.baseUrl}/v1/videos/${encodeURIComponent(videoRef)}`)
    if (response.status === 404) {
      return null
    }
    if (!response.ok) {
      throw { code: 'DETAIL_UNAVAILABLE', message: 'detail unavailable' }
    }
    return readDetail(asRecord(await response.json()))
  }

  async simulateUpload(): Promise<void> {
    throw { code: 'UPLOAD_UNAVAILABLE', message: 'upload is not part of this adapter' }
  }
}

function readItem(value: unknown): VideoLibraryItem {
  const record = asRecord(value)
  return {
    videoRef: asString(record.videoRef),
    originalFilename: asString(record.originalFilename),
    status: asVideoStatus(record.status),
    submittedAt: asString(record.submittedAt),
    activityAt: asString(record.activityAt),
  }
}

function readDetail(record: Record<string, unknown>): VideoDetail {
  const item = readItem(record)
  return {
    ...item,
    uploadedAt: record.uploadedAt === null ? null : asString(record.uploadedAt),
    processing: readProcessing(record.processing),
  }
}

function readProcessing(value: unknown): VideoDetail['processing'] {
  if (value === null || value === undefined) {
    return null
  }
  const record = asRecord(value)
  return {
    status: asProcessingStatus(record.status),
    requestedAt: asString(record.requestedAt),
    startedAt: record.startedAt === null ? null : asString(record.startedAt),
    finishedAt: record.finishedAt === null ? null : asString(record.finishedAt),
  }
}

function asRecord(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw { code: 'LIST_UNAVAILABLE', message: 'invalid payload' }
  }
  return value as Record<string, unknown>
}

function asArray(value: unknown): unknown[] {
  return Array.isArray(value) ? value : []
}

function asString(value: unknown): string {
  if (typeof value !== 'string' || value.length === 0) {
    throw { code: 'LIST_UNAVAILABLE', message: 'invalid payload' }
  }
  return value
}

function asNumber(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback
}

function asVideoStatus(value: unknown): ProductVideoStatus {
  if (typeof value === 'string' && (PRODUCT_VIDEO_STATUSES as readonly string[]).includes(value)) {
    return value as ProductVideoStatus
  }
  throw { code: 'LIST_UNAVAILABLE', message: 'invalid payload' }
}

function asProcessingStatus(value: unknown): ProductProcessingStatus {
  if (typeof value === 'string' && (PRODUCT_PROCESSING_STATUSES as readonly string[]).includes(value)) {
    return value as ProductProcessingStatus
  }
  throw { code: 'LIST_UNAVAILABLE', message: 'invalid payload' }
}
