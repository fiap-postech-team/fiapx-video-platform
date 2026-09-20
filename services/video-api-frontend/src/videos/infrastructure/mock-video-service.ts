import { compareVideos } from '../application/product-status'
import type {
  PrototypeScenario,
  UploadSelection,
  Video,
  VideoPage,
  VideoService,
} from '../domain/video'
import { demoVideos } from './fixtures'

const DEFAULT_LIMIT = 20
const MAX_LIMIT = 100

function cloneVideos(videos: Video[]): Video[] {
  return videos.map((video) => ({
    ...video,
    attempts: video.attempts.map((attempt) => ({ ...attempt })),
  }))
}

function wait(ms: number): Promise<void> {
  if (ms <= 0) {
    return Promise.resolve()
  }
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

export class MockVideoService implements VideoService {
  private readonly videos: Video[]

  constructor(videos: Video[] = demoVideos()) {
    this.videos = cloneVideos(videos)
  }

  async list(
    userId: string,
    query?: { cursor?: string; limit?: number; scenario?: PrototypeScenario },
  ): Promise<VideoPage> {
    const scenario = query?.scenario
    await wait(scenario?.delayMs ?? 0)

    if (scenario?.kind === 'loading') {
      return new Promise(() => undefined)
    }
    if (scenario?.kind === 'list-error') {
      return Promise.reject({
        code: 'LIST_UNAVAILABLE',
        message: 'mock scenario: list unavailable',
      })
    }

    const limit = query?.limit ?? DEFAULT_LIMIT
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new Error('Invalid page size')
    }

    const owned = (scenario?.kind === 'empty' ? [] : this.videos.filter((video) => video.ownerId === userId))
      .slice()
      .sort(compareVideos)

    const start = query?.cursor ? owned.findIndex((video) => video.id === query.cursor) + 1 : 0
    const sliceStart = start < 1 && query?.cursor ? owned.length : start
    const items = owned.slice(sliceStart, sliceStart + limit)
    const last = items.at(-1)
    const hasMore = sliceStart + items.length < owned.length

    return {
      items: cloneVideos(items),
      nextCursor: hasMore && last ? last.id : null,
    }
  }

  async get(userId: string, videoId: string): Promise<Video | null> {
    const video = this.videos.find((item) => item.id === videoId && item.ownerId === userId)
    return video ? cloneVideos([video])[0] ?? null : null
  }

  async simulateUpload(
    userId: string,
    selection: UploadSelection,
    options?: { scenario?: PrototypeScenario; onProgress?: (percent: number) => void },
  ): Promise<Video> {
    const scenario = options?.scenario
    const delayMs = scenario?.delayMs ?? 0
    options?.onProgress?.(20)
    await wait(delayMs)
    if (scenario?.kind === 'upload-error') {
      return Promise.reject({
        code: 'UPLOAD_UNAVAILABLE',
        message: 'mock scenario: upload unavailable',
      })
    }
    options?.onProgress?.(70)
    await wait(delayMs)
    options?.onProgress?.(100)

    const now = '2026-09-20T12:00:00Z'
    const video: Video = {
      id: crypto.randomUUID(),
      ownerId: userId,
      originalFilename: selection.name,
      sizeBytes: selection.sizeBytes,
      contentType: selection.contentType,
      uploadStatus: 'UPLOADED',
      requestedAt: now,
      uploadedAt: now,
      attempts: [],
    }
    this.videos.unshift(video)
    return cloneVideos([video])[0]!
  }
}
