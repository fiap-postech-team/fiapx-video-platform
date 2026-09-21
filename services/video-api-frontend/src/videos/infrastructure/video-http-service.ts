import { videoApiBaseUrl } from '../../auth/infrastructure/api-base-url'
import { validateUploadSelection } from '../application/validate-upload'
import {
  PRODUCT_PROCESSING_STATUSES,
  PRODUCT_VIDEO_STATUSES,
  type ProductProcessingStatus,
  type ProductVideoStatus,
  UploadFailure,
  type UploadJobStatus,
  type UploadOptions,
  type UploadResult,
  type VideoDetail,
  type VideoLibraryItem,
  type VideoLibraryPage,
  type VideoService,
} from '../domain/video'

export type AuthorizedFetch = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>
type XhrFactory = () => XMLHttpRequest
type Wait = (milliseconds: number, signal?: AbortSignal) => Promise<void>

export class VideoHttpService implements VideoService {
  private readonly baseUrl: string
  private readonly authorizedFetch: AuthorizedFetch
  private readonly xhrFactory: XhrFactory
  private readonly wait: Wait

  constructor(authorizedFetch: AuthorizedFetch, baseUrl?: string, options: { xhrFactory?: XhrFactory; wait?: Wait } = {}) {
    this.authorizedFetch = authorizedFetch
    this.baseUrl = videoApiBaseUrl(baseUrl)
    this.xhrFactory = options.xhrFactory ?? (() => new XMLHttpRequest())
    this.wait = options.wait ?? waitFor
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

  async upload(file: File, options: UploadOptions): Promise<UploadResult> {
    const reservation = await this.reserve(file)
    options.onPhase('sending')
    try {
      await putFile(this.xhrFactory, reservation.uploadUrl, reservation.headers, file, options)
    } catch (error) {
      if (options.signal?.aborted) throw error
      // A lost PUT response is not proof of failure: confirmation is the source of truth.
      options.onPhase('confirming')
      const confirmed = await this.confirmIfPresent(reservation.videoId)
      if (!confirmed) {
        if (Date.now() >= Date.parse(reservation.expiresAt)) throw new UploadFailure('UPLOAD_EXPIRED')
        throw new UploadFailure('UPLOAD_UNCERTAIN')
      }
      return this.startAndTrack(confirmed, options)
    }
    options.onPhase('confirming')
    const confirmed = await this.confirm(reservation.videoId)
    return this.startAndTrack(confirmed, options)
  }

  private async reserve(file: File): Promise<UploadReservation> {
    const validation = validateUploadSelection({ name: file.name, sizeBytes: file.size, contentType: file.type })
    if (!validation.selection) throw new UploadFailure('UPLOAD_UNAVAILABLE')
    const response = await this.authorizedFetch(`${this.baseUrl}/v1/videos/uploads`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        originalFilename: validation.selection.name,
        contentType: validation.selection.contentType,
        sizeBytes: validation.selection.sizeBytes,
      }),
    })
    if (!response.ok) throw new UploadFailure(response.status === 410 ? 'UPLOAD_EXPIRED' : 'UPLOAD_UNAVAILABLE')
    return readReservation(asRecord(await response.json()))
  }

  private async confirm(videoId: string): Promise<ConfirmedVideo> {
    const response = await this.authorizedFetch(`${this.baseUrl}/v1/videos/${encodeURIComponent(videoId)}/confirm`, { method: 'POST' })
    if (!response.ok) throw new UploadFailure(response.status === 410 ? 'UPLOAD_EXPIRED' : 'UPLOAD_UNAVAILABLE')
    return readConfirmed(asRecord(await response.json()))
  }

  private async confirmIfPresent(videoId: string): Promise<ConfirmedVideo | null> {
    try { return await this.confirm(videoId) } catch { return null }
  }

  private async startAndTrack(confirmed: ConfirmedVideo, options: UploadOptions): Promise<UploadResult> {
    options.onPhase('processing')
    const job = await this.createJob(confirmed.sourceKey)
    let current = job
    options.onJobStatus(current.status)
    while (current.status === 'PENDING' || current.status === 'PROCESSING') {
      await this.wait(3_000, options.signal)
      current = await this.getJob(current.id)
      options.onJobStatus(current.status)
    }
    return { videoId: confirmed.videoId, jobId: current.id, status: current.status }
  }

  private async createJob(sourceKey: string): Promise<Job> {
    const request = () => this.authorizedFetch(`${this.baseUrl}/v1/jobs`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sourceKey }),
    })
    let response: Response
    try { response = await request() } catch { response = await request() }
    if (!response.ok) throw new UploadFailure('UPLOAD_UNAVAILABLE')
    return readJob(asRecord(await response.json()))
  }

  private async getJob(jobId: string): Promise<Job> {
    const response = await this.authorizedFetch(`${this.baseUrl}/v1/jobs/${encodeURIComponent(jobId)}`)
    if (!response.ok) throw new UploadFailure('UPLOAD_UNAVAILABLE')
    return readJob(asRecord(await response.json()))
  }
}

interface UploadReservation { videoId: string; uploadUrl: string; expiresAt: string; headers: Record<string, string> }
interface ConfirmedVideo { videoId: string; sourceKey: string }
interface Job { id: string; status: UploadJobStatus }
function readReservation(value: Record<string, unknown>): UploadReservation { return { videoId: asString(value.videoId), uploadUrl: asString(value.uploadUrl), expiresAt: asString(value.expiresAt), headers: asHeaders(value.headers) } }
function readConfirmed(value: Record<string, unknown>): ConfirmedVideo { return { videoId: asString(value.videoId), sourceKey: asString(value.sourceKey) } }
function readJob(value: Record<string, unknown>): Job { const status = asString(value.status); if (!['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'].includes(status)) throw new UploadFailure('UPLOAD_UNAVAILABLE'); return { id: asString(value.id), status: status as UploadJobStatus } }
function asHeaders(value: unknown): Record<string, string> { const input = asRecord(value); return Object.fromEntries(Object.entries(input).map(([key, item]) => [key, asString(item)])) }
function putFile(factory: XhrFactory, url: string, headers: Record<string, string>, file: File, options: UploadOptions): Promise<void> {
  return new Promise((resolve, reject) => {
    const request = factory()
    const cleanup = () => options.signal?.removeEventListener('abort', abort)
    const abort = () => request.abort()
    const fail = (error: Error) => { cleanup(); reject(error) }
    options.signal?.addEventListener('abort', abort, { once: true })
    request.open('PUT', url)
    Object.entries(headers).forEach(([key, value]) => request.setRequestHeader(key, value))
    request.upload.onprogress = event => {
      if (event.lengthComputable) options.onProgress(Math.round((event.loaded / event.total) * 100))
    }
    request.onload = () => {
      cleanup()
      if (request.status >= 200 && request.status < 300) resolve()
      else reject(new Error(`upload ${request.status}`))
    }
    request.onerror = () => fail(new Error('upload network error'))
    request.onabort = () => fail(new DOMException('Aborted', 'AbortError'))
    request.ontimeout = () => fail(new Error('upload timeout'))
    request.send(file)
  })
}

function waitFor(milliseconds: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    const abort = () => {
      clearTimeout(timer)
      reject(new DOMException('Aborted', 'AbortError'))
    }
    const timer = setTimeout(() => {
      signal?.removeEventListener('abort', abort)
      resolve()
    }, milliseconds)
    signal?.addEventListener('abort', abort, { once: true })
  })
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
  const submittedAt = asString(record.submittedAt)
  const uploadedAt = record.uploadedAt == null ? null : asString(record.uploadedAt)
  const processing = readProcessing(record.processing)
  return {
    videoRef: asString(record.videoRef),
    originalFilename: asString(record.originalFilename),
    status: asVideoStatus(record.status),
    submittedAt,
    activityAt: activityAt(record.activityAt, processing, uploadedAt, submittedAt),
    uploadedAt,
    processing,
  }
}

function activityAt(
  value: unknown,
  processing: VideoDetail['processing'],
  uploadedAt: string | null,
  submittedAt: string,
): string {
  if (typeof value === 'string' && value.length > 0) {
    return value
  }
  return processing?.finishedAt ?? processing?.startedAt ?? processing?.requestedAt ?? uploadedAt ?? submittedAt
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
