import { describe, expect, it, vi } from 'vitest'
import { VideoHttpService } from './video-http-service'

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('VideoHttpService', () => {
  it('maps a numbered page without leaking internal fields', async () => {
    const authorizedFetch = vi.fn().mockResolvedValue(json(200, {
      items: [{
        videoRef: 't7nsTQEuS4KPGL-Cz5DZaw',
        originalFilename: 'aula-01.mp4',
        status: 'AVAILABLE',
        submittedAt: '2026-09-20T14:10:00Z',
        activityAt: '2026-09-20T14:16:42Z',
      }],
      page: 1,
      pageSize: 5,
      totalItems: 1,
      totalPages: 1,
    }))
    const service = new VideoHttpService(authorizedFetch, 'http://localhost:8080')

    const page = await service.list(1)

    expect(authorizedFetch).toHaveBeenCalledWith('http://localhost:8080/v1/videos?page=1')
    expect(page.items[0]).toEqual({
      videoRef: 't7nsTQEuS4KPGL-Cz5DZaw',
      originalFilename: 'aula-01.mp4',
      status: 'AVAILABLE',
      submittedAt: '2026-09-20T14:10:00Z',
      activityAt: '2026-09-20T14:16:42Z',
    })
    expect(JSON.stringify(page)).not.toMatch(/sourceKey|resultKey|userId/)
  })

  it('normalizes a missing processing object and treats 404 as not found', async () => {
    const authorizedFetch = vi.fn()
      .mockResolvedValueOnce(json(200, {
        videoRef: 't7nsTQEuS4KPGL-Cz5DZaw',
        originalFilename: 'aula-01.mp4',
        status: 'UPLOADED',
        submittedAt: '2026-09-20T14:10:00Z',
        activityAt: '2026-09-20T14:10:00Z',
        uploadedAt: '2026-09-20T14:11:12Z',
        processing: null,
      }))
      .mockResolvedValueOnce(json(404, { code: 'NOT_FOUND' }))
    const service = new VideoHttpService(authorizedFetch, 'http://localhost:8080')

    const detail = await service.get('t7nsTQEuS4KPGL-Cz5DZaw')
    expect(detail?.processing).toBeNull()
    await expect(service.get('missing')).resolves.toBeNull()
  })
})
