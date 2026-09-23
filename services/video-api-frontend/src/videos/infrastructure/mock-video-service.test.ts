import { describe, expect, it } from 'vitest'
import { DEMO_USER } from '../../auth/infrastructure/mock-authentication-service'
import { MockVideoService } from './mock-video-service'

describe('MockVideoService', () => {
  it('lists one row per video ordered by activity and paginates by number', async () => {
    const service = new MockVideoService()
    const first = await service.list(1)
    expect(first.items.map((video) => video.originalFilename)).toEqual([
      'aula-gravada.mp4',
      'apresentacao.mp4',
      'reuniao.webm',
      'treino.mov',
      'campanha.mp4',
    ])
    expect(first.pageSize).toBe(5)
    expect(first.totalItems).toBe(8)
    expect(first.totalPages).toBe(2)
    const second = await service.list(2)
    expect(second.items.map((video) => video.originalFilename)).toEqual([
      'entrevista.mp4',
      'material.mp4',
      'rascunho.mp4',
    ])
  })

  it('hides an unknown reference and keeps a single processing on the video', async () => {
    const service = new MockVideoService()
    const video = await service.get('ref-available')
    expect(video?.processing?.status).toBe('AVAILABLE')
    expect(video?.processing).not.toBeNull()
    await expect(service.get('missing')).resolves.toBeNull()
  })

  it('creates a local uploaded video from metadata only', async () => {
    const service = new MockVideoService()
    const percents: number[] = []
    await service.simulateUpload(
      DEMO_USER.id,
      { name: 'gravacao.webm', sizeBytes: 73_400_320, contentType: 'video/webm' },
      { onProgress: (percent) => percents.push(percent) },
    )
    expect(percents.at(-1)).toBe(100)
    const page = await service.list(1)
    expect(page.items[0]?.originalFilename).toBe('gravacao.webm')
    expect(page.items[0]?.status).toBe('UPLOADED')
  })

  it('rejects list and upload according to the selected scenario', async () => {
    const service = new MockVideoService()
    await expect(service.list(1, { scenario: { kind: 'list-error' } })).rejects.toMatchObject({
      code: 'LIST_UNAVAILABLE',
    })
    await expect(
      service.simulateUpload(DEMO_USER.id, { name: 'aula.mp4', sizeBytes: 10, contentType: 'video/mp4' }, {
        scenario: { kind: 'upload-error' },
      }),
    ).rejects.toMatchObject({ code: 'UPLOAD_UNAVAILABLE' })
    await expect(service.list(1, { scenario: { kind: 'empty' } })).resolves.toMatchObject({
      items: [],
      totalPages: 0,
    })
  })
})
