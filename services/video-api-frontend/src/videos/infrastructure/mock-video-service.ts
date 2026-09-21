import type {
  PrototypeScenario,
  UploadSelection,
  VideoDetail,
  VideoLibraryItem,
  VideoLibraryPage,
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

export class MockVideoService implements VideoService {
  private readonly videos: DemoVideo[]

  constructor(videos: DemoVideo[] = demoVideos()) {
    this.videos = videos.map(clone)
  }

  async list(page = 1, options?: { scenario?: PrototypeScenario }): Promise<VideoLibraryPage> {
    const scenario = options?.scenario
    await wait(scenario?.delayMs ?? 0)
    if (scenario?.kind === 'loading') {
      return new Promise(() => undefined)
    }
    if (scenario?.kind === 'list-error') {
      return Promise.reject({ code: 'LIST_UNAVAILABLE', message: 'mock scenario: list unavailable' })
    }
    const owned = (scenario?.kind === 'empty' ? [] : this.videos.map((video) => video.item))
      .slice()
      .sort(byActivity)
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
