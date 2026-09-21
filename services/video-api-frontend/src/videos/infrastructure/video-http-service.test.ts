import { describe, expect, it, vi } from 'vitest'
import { UploadFailure } from '../domain/video'
import { VideoHttpService } from './video-http-service'

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function fakeXhr(outcome: 'success' | 'network-error' = 'success') {
  const upload = { onprogress: null as ((event: ProgressEvent) => void) | null }
  const request = {
    status: 200,
    upload,
    open: vi.fn(),
    setRequestHeader: vi.fn(),
    abort: vi.fn(),
    onload: null as (() => void) | null,
    onerror: null as (() => void) | null,
    onabort: null as (() => void) | null,
    ontimeout: null as (() => void) | null,
    send: vi.fn(() => {
      upload.onprogress?.({ lengthComputable: true, loaded: 3, total: 4 } as ProgressEvent)
      if (outcome === 'success') request.onload?.()
      else request.onerror?.()
    }),
  }
  return request
}

const reservation = {
  videoId: 'be7588bd-32b0-482a-a2d1-021f0258ca1d',
  sourceKey: 'users/u/videos/v/source',
  uploadUrl: 'http://localhost:8080/_local/mock-storage/uploads/opaque-capability',
  expiresAt: '2099-09-21T02:15:00Z',
  method: 'PUT',
  headers: { 'Content-Type': 'video/mp4', 'If-None-Match': '*' },
}

const confirmed = { videoId: reservation.videoId, sourceKey: reservation.sourceKey, status: 'UPLOADED' }
const completedJob = { id: 'job-1', status: 'COMPLETED' }

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

  it('sends exact storage headers, reports byte progress and tracks the job to completion', async () => {
    const authorizedFetch = vi.fn()
      .mockResolvedValueOnce(json(201, reservation))
      .mockResolvedValueOnce(json(200, confirmed))
      .mockResolvedValueOnce(json(201, { id: 'job-1', status: 'PENDING' }))
      .mockResolvedValueOnce(json(200, completedJob))
    const request = fakeXhr()
    const wait = vi.fn().mockResolvedValue(undefined)
    const onProgress = vi.fn()
    const onPhase = vi.fn()
    const onJobStatus = vi.fn()
    const service = new VideoHttpService(authorizedFetch, 'http://localhost:8080', {
      xhrFactory: () => request as unknown as XMLHttpRequest,
      wait,
    })

    await expect(service.upload(new File(['video'], 'aula.mp4', { type: 'video/mp4' }), {
      onProgress, onPhase, onJobStatus,
    })).resolves.toEqual({ videoId: reservation.videoId, jobId: 'job-1', status: 'COMPLETED' })

    expect(request.open).toHaveBeenCalledWith('PUT', reservation.uploadUrl)
    expect(authorizedFetch).toHaveBeenNthCalledWith(
      1,
      'http://localhost:8080/v1/videos/uploads',
      expect.objectContaining({
        body: JSON.stringify({ originalFilename: 'aula.mp4', contentType: 'video/mp4', sizeBytes: 5 }),
      }),
    )
    expect(request.setRequestHeader.mock.calls).toEqual([
      ['Content-Type', 'video/mp4'],
      ['If-None-Match', '*'],
    ])
    expect(request.setRequestHeader).not.toHaveBeenCalledWith('Authorization', expect.anything())
    expect(onProgress).toHaveBeenCalledWith(75)
    expect(onPhase.mock.calls.flat()).toEqual(['sending', 'confirming', 'processing'])
    expect(onJobStatus.mock.calls.flat()).toEqual(['PENDING', 'COMPLETED'])
    expect(wait).toHaveBeenCalledWith(3_000, undefined)
  })

  it('confirms after an uncertain PUT and does not transfer the bytes again', async () => {
    const authorizedFetch = vi.fn()
      .mockResolvedValueOnce(json(201, reservation))
      .mockResolvedValueOnce(json(200, confirmed))
      .mockResolvedValueOnce(json(201, completedJob))
    const request = fakeXhr('network-error')
    const service = new VideoHttpService(authorizedFetch, 'http://localhost:8080', {
      xhrFactory: () => request as unknown as XMLHttpRequest,
    })

    await expect(service.upload(new File(['video'], 'aula.mp4', { type: 'video/mp4' }), {
      onProgress: vi.fn(), onPhase: vi.fn(), onJobStatus: vi.fn(),
    })).resolves.toMatchObject({ status: 'COMPLETED' })

    expect(request.send).toHaveBeenCalledTimes(1)
    expect(authorizedFetch).toHaveBeenNthCalledWith(
      2,
      `http://localhost:8080/v1/videos/${reservation.videoId}/confirm`,
      { method: 'POST' },
    )
  })

  it('requires a fresh reservation after the URL expires', async () => {
    const expiredReservation = { ...reservation, expiresAt: '2000-01-01T00:00:00Z' }
    const authorizedFetch = vi.fn()
      .mockResolvedValueOnce(json(201, expiredReservation))
      .mockResolvedValueOnce(json(409, { code: 'CONFLICT' }))
    const request = fakeXhr('network-error')
    const service = new VideoHttpService(authorizedFetch, 'http://localhost:8080', {
      xhrFactory: () => request as unknown as XMLHttpRequest,
    })

    await expect(service.upload(new File(['video'], 'aula.mp4', { type: 'video/mp4' }), {
      onProgress: vi.fn(), onPhase: vi.fn(), onJobStatus: vi.fn(),
    })).rejects.toEqual(new UploadFailure('UPLOAD_EXPIRED'))

    expect(request.send).toHaveBeenCalledTimes(1)
  })

  it('repeats an uncertain job creation with the same idempotency key', async () => {
    const authorizedFetch = vi.fn()
      .mockResolvedValueOnce(json(201, reservation))
      .mockResolvedValueOnce(json(200, confirmed))
      .mockRejectedValueOnce(new TypeError('network response lost'))
      .mockResolvedValueOnce(json(200, completedJob))
    const request = fakeXhr()
    const service = new VideoHttpService(authorizedFetch, 'http://localhost:8080', {
      xhrFactory: () => request as unknown as XMLHttpRequest,
    })

    await service.upload(new File(['video'], 'aula.mp4', { type: 'video/mp4' }), {
      onProgress: vi.fn(), onPhase: vi.fn(), onJobStatus: vi.fn(),
    })

    const jobCalls = authorizedFetch.mock.calls.filter(([url]) => url === 'http://localhost:8080/v1/jobs')
    expect(jobCalls).toHaveLength(2)
    expect(jobCalls[0]?.[1]).toEqual(jobCalls[1]?.[1])
    expect(jobCalls[0]?.[1]).toMatchObject({
      headers: { 'Content-Type': 'application/json', 'Idempotency-Key': `video:${reservation.videoId}` },
    })
  })

  it('normalizes an empty browser MIME before reserving an MKV upload', async () => {
    const mkvReservation = {
      ...reservation,
      headers: { 'Content-Type': 'video/x-matroska', 'If-None-Match': '*' },
    }
    const authorizedFetch = vi.fn()
      .mockResolvedValueOnce(json(201, mkvReservation))
      .mockResolvedValueOnce(json(200, confirmed))
      .mockResolvedValueOnce(json(201, completedJob))
    const request = fakeXhr()
    const service = new VideoHttpService(authorizedFetch, 'http://localhost:8080', {
      xhrFactory: () => request as unknown as XMLHttpRequest,
    })

    await service.upload(new File(['video'], 'aula.mkv'), {
      onProgress: vi.fn(), onPhase: vi.fn(), onJobStatus: vi.fn(),
    })

    expect(authorizedFetch.mock.calls[0]?.[1]).toMatchObject({
      body: JSON.stringify({ originalFilename: 'aula.mkv', contentType: 'video/x-matroska', sizeBytes: 5 }),
    })
  })

  it('returns a terminal processing failure without turning it into success', async () => {
    const authorizedFetch = vi.fn()
      .mockResolvedValueOnce(json(201, reservation))
      .mockResolvedValueOnce(json(200, confirmed))
      .mockResolvedValueOnce(json(201, { id: 'job-1', status: 'FAILED' }))
    const request = fakeXhr()
    const service = new VideoHttpService(authorizedFetch, 'http://localhost:8080', {
      xhrFactory: () => request as unknown as XMLHttpRequest,
    })

    await expect(service.upload(new File(['video'], 'aula.mp4', { type: 'video/mp4' }), {
      onProgress: vi.fn(), onPhase: vi.fn(), onJobStatus: vi.fn(),
    })).resolves.toMatchObject({ status: 'FAILED' })
  })
})
