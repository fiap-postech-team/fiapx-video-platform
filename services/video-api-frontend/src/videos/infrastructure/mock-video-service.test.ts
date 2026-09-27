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

  it('filters names and status groups before calculating pagination', async () => {
    const service = new MockVideoService()

    await expect(service.list(1, {
      query: { name: '  A  ', match: 'PREFIX', status: 'ALL' },
    })).resolves.toMatchObject({
      items: [
        expect.objectContaining({ originalFilename: 'aula-gravada.mp4' }),
        expect.objectContaining({ originalFilename: 'apresentacao.mp4' }),
      ],
      totalItems: 2,
      totalPages: 1,
    })
    await expect(service.list(1, {
      query: { name: 'CAMPANHA.MP4', match: 'EXACT', status: 'PROCESSED' },
    })).resolves.toMatchObject({
      items: [expect.objectContaining({ originalFilename: 'campanha.mp4' })],
      totalItems: 1,
    })
    await expect(service.list(1, {
      query: { status: 'PROCESSING' },
    })).resolves.toMatchObject({ totalItems: 3, totalPages: 1 })
    await expect(service.list(1, {
      query: { status: 'FAILED' },
    })).resolves.toMatchObject({ totalItems: 3, totalPages: 1 })
  })

  it('sorts the complete filtered collection before pagination', async () => {
    const service = new MockVideoService()

    const ascending = await service.list(1, {
      query: { status: 'ALL', sort: 'UPDATED_AT', direction: 'ASC' },
    })
    expect(ascending.items[0]?.originalFilename).toBe('rascunho.mp4')

    const byStatus = await service.list(1, {
      query: { status: 'ALL', sort: 'STATUS', direction: 'ASC' },
    })
    expect(byStatus.items.map((video) => video.status)).toEqual([
      'EXPIRED', 'FAILED', 'AWAITING_UPLOAD', 'AVAILABLE', 'UPLOADED',
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
