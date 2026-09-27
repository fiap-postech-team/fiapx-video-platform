import type {
  PrototypeScenario,
  DownloadResult,
  UploadSelection,
  VideoDetail,
  VideoLibraryItem,
  VideoLibraryPage,
  VideoLibraryQuery,
  VideoListOptions,
  VideoService,
} from '../domain/video'
import { demoVideos, type DemoVideo } from './fixtures'

const PAGE_SIZE = 5

function clone(video: DemoVideo): DemoVideo {
  return {
    item: { ...video.item },
    detail: {
      ...video.detail,
      processing: video.detail.processing ? { ...video.detail.processing } : null,
    },
  }
}

function wait(ms: number): Promise<void> {
  if (ms <= 0) {
    return Promise.resolve()
  }
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

function byActivity(left: VideoLibraryItem, right: VideoLibraryItem): number {
  return right.activityAt.localeCompare(left.activityAt) || right.videoRef.localeCompare(left.videoRef)
}

const STATUS_ORDER: Record<VideoLibraryItem['status'], number> = {
  EXPIRED: 1,
  FAILED: 2,
  AWAITING_UPLOAD: 3,
  AVAILABLE: 4,
  UPLOADED: 5,
  PROCESSING: 5,
  REJECTED: 6,
}

function byLibraryOrder(query?: VideoLibraryQuery) {
  const direction = query?.direction === 'ASC' ? 1 : -1
  return (left: VideoLibraryItem, right: VideoLibraryItem): number => {
    if (query?.sort === 'STATUS') {
      const statusOrder = (STATUS_ORDER[left.status] - STATUS_ORDER[right.status]) * direction
      if (statusOrder !== 0) return statusOrder
    } else {
      const activityOrder = left.activityAt.localeCompare(right.activityAt) * direction
      if (activityOrder !== 0) return activityOrder
    }
    return byActivity(left, right)
  }
}

function matchesName(video: VideoLibraryItem, query?: VideoLibraryQuery): boolean {
  const name = query?.name?.trim().toLocaleLowerCase('pt-BR')
  if (!name) {
    return true
  }
  const filename = video.originalFilename.toLocaleLowerCase('pt-BR')
  return query?.match === 'EXACT' ? filename === name : filename.startsWith(name)
}

function matchesStatus(video: VideoLibraryItem, query?: VideoLibraryQuery): boolean {
  switch (query?.status ?? 'ALL') {
    case 'PROCESSED':
      return video.status === 'AVAILABLE'
    case 'PROCESSING':
      return video.status === 'UPLOADED' || video.status === 'PROCESSING'
    case 'FAILED':
      return video.status === 'FAILED' || video.status === 'REJECTED' || video.status === 'EXPIRED'
    default:
      return true
  }
}

export class MockVideoService implements VideoService {
  private readonly videos: DemoVideo[]

  constructor(videos: DemoVideo[] = demoVideos()) {
    this.videos = videos.map(clone)
  }

  async list(page = 1, options?: VideoListOptions): Promise<VideoLibraryPage> {
    const scenario = options?.scenario
    await wait(scenario?.delayMs ?? 0)
    if (scenario?.kind === 'loading') {
      return new Promise(() => undefined)
    }
    if (scenario?.kind === 'list-error') {
      return Promise.reject({ code: 'LIST_UNAVAILABLE', message: 'mock scenario: list unavailable' })
    }
    const owned = (scenario?.kind === 'empty' ? [] : this.videos.map((video) => video.item))
      .filter((video) => matchesName(video, options?.query) && matchesStatus(video, options?.query))
      .slice()
      .sort(byLibraryOrder(options?.query))
    const totalItems = owned.length
    const totalPages = totalItems === 0 ? 0 : Math.ceil(totalItems / PAGE_SIZE)
    const start = Math.max(0, (page - 1) * PAGE_SIZE)
    return {
      items: owned.slice(start, start + PAGE_SIZE).map((item) => ({ ...item })),
      page,
      pageSize: PAGE_SIZE,
      totalItems,
      totalPages,
    }
  }

  async get(videoRef: string): Promise<VideoDetail | null> {
    const video = this.videos.find((item) => item.item.videoRef === videoRef)
    return video ? clone(video).detail : null
  }

  async download(jobId: string): Promise<DownloadResult> {
    const video = this.videos.find((item) => item.item.jobId === jobId)
    if (!video || video.item.status !== 'AVAILABLE') {
      throw { code: 'DOWNLOAD_NOT_READY', message: 'mock result is not ready' }
    }
    return {
      downloadUrl: 'https://shorturl.at/JpxZS',
      expiresAt: '2099-01-01T00:00:00Z',
      filename: `${video.item.originalFilename.replace(/\.[^.]+$/, '')}-${jobId}.zip`,
      contentType: 'application/zip',
      sizeBytes: 128,
    }
  }

  async simulateUpload(
    _userId: string,
    selection: UploadSelection,
    options?: { scenario?: PrototypeScenario; onProgress?: (percent: number) => void },
  ): Promise<void> {
    const scenario = options?.scenario
    const delayMs = scenario?.delayMs ?? 0
    options?.onProgress?.(20)
    await wait(delayMs)
    if (scenario?.kind === 'upload-error') {
      return Promise.reject({ code: 'UPLOAD_UNAVAILABLE', message: 'mock scenario: upload unavailable' })
    }
    options?.onProgress?.(70)
    await wait(delayMs)
    options?.onProgress?.(100)
    const now = '2026-09-20T12:00:00Z'
    const videoRef = `ref-${selection.name}`
    this.videos.unshift(clone({
      item: {
        videoRef,
        originalFilename: selection.name,
        status: 'UPLOADED',
        jobId: null,
        submittedAt: now,
        activityAt: now,
      },
      detail: {
        videoRef,
        originalFilename: selection.name,
        status: 'UPLOADED',
        submittedAt: now,
        activityAt: now,
        uploadedAt: now,
        processing: null,
      },
    }))
  }
}
