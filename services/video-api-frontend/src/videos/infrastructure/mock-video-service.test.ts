import { describe, expect, it } from 'vitest'
import { DEMO_USER } from '../../auth/infrastructure/mock-authentication-service'
import { processingStatus } from '../application/product-status'
import { MockVideoService } from './mock-video-service'

describe('MockVideoService', () => {
  it('lists one row per owned video, newest first, and paginates', async () => {
    const service = new MockVideoService()
    const first = await service.list(DEMO_USER.id, { limit: 3 })
    expect(first.items).toHaveLength(3)
    expect(first.items.map((video) => video.originalFilename)).toEqual([
      'aula-gravada.mp4',
      'apresentacao.mp4',
      'reuniao.webm',
    ])
    expect(first.nextCursor).toBe('video-queued')

    const second = await service.list(DEMO_USER.id, { cursor: first.nextCursor ?? undefined, limit: 3 })
    expect(second.items).toHaveLength(3)
    expect(await service.list('another-owner')).toEqual({ items: [], nextCursor: null })
  })

  it('hides another owner as not found and keeps processing history on the video', async () => {
    const service = new MockVideoService()
    const video = await service.get(DEMO_USER.id, 'video-available')
    expect(processingStatus(video!)).toBe('completed')
    expect(video?.attempts).toHaveLength(2)
    await expect(service.get('another-owner', 'video-available')).resolves.toBeNull()
  })

  it('creates a local uploaded video from metadata only', async () => {
    const service = new MockVideoService()
    const percents: number[] = []
    const created = await service.simulateUpload(
      DEMO_USER.id,
      { name: 'gravacao.webm', sizeBytes: 73_400_320, contentType: 'video/webm' },
      { onProgress: (percent) => percents.push(percent) },
    )
    expect(created.originalFilename).toBe('gravacao.webm')
    expect(created.uploadStatus).toBe('UPLOADED')
    expect(percents.at(-1)).toBe(100)
    const page = await service.list(DEMO_USER.id)
    expect(page.items[0]?.originalFilename).toBe('gravacao.webm')
  })

  it('rejects list and upload according to the selected scenario', async () => {
    const service = new MockVideoService()
    await expect(service.list(DEMO_USER.id, { scenario: { kind: 'list-error' } })).rejects.toMatchObject({
      code: 'LIST_UNAVAILABLE',
    })
    await expect(
      service.simulateUpload(DEMO_USER.id, { name: 'aula.mp4', sizeBytes: 10, contentType: 'video/mp4' }, {
        scenario: { kind: 'upload-error' },
      }),
    ).rejects.toMatchObject({ code: 'UPLOAD_UNAVAILABLE' })
    await expect(service.list(DEMO_USER.id, { scenario: { kind: 'empty' } })).resolves.toEqual({
      items: [],
      nextCursor: null,
    })
  })
})
