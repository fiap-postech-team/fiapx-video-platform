import { describe, expect, it } from 'vitest'
import { DEMO_USER } from '../../auth/infrastructure/mock-authentication-service'
import { CONFIRMED_SOURCE_KEY, MockJobService, UNCONFIRMED_SOURCE_KEY } from './mock-job-service'

describe('MockJobService', () => {
  it('lists only jobs of the owner and paginates by cursor', async () => {
    const service = new MockJobService(() => '2026-09-19T12:00:00Z')

    const first = await service.list(DEMO_USER.id, { limit: 2 })
    expect(first.items).toHaveLength(2)
    expect(first.nextCursor).toBe(first.items[1]?.id)

    const second = await service.list(DEMO_USER.id, { cursor: first.nextCursor ?? undefined, limit: 2 })
    expect(second.items).toHaveLength(2)
    expect(second.nextCursor).toBeNull()
    expect(await service.list('another-owner')).toEqual({ items: [], nextCursor: null })
  })

  it('hides jobs of another owner as not found', async () => {
    const service = new MockJobService()
    const page = await service.list(DEMO_USER.id)

    await expect(service.get('another-owner', page.items[0]!.id)).resolves.toBeNull()
  })

  it('creates a pending job for a confirmed source key', async () => {
    const service = new MockJobService()
    const result = await service.create(DEMO_USER.id, { sourceKey: CONFIRMED_SOURCE_KEY })

    expect(result).toMatchObject({ job: { sourceKey: CONFIRMED_SOURCE_KEY, status: 'PENDING', userId: DEMO_USER.id } })
  })

  it('rejects unconfirmed and missing videos', async () => {
    const service = new MockJobService()

    await expect(service.create(DEMO_USER.id, { sourceKey: UNCONFIRMED_SOURCE_KEY })).resolves.toMatchObject({
      error: { code: 'VIDEO_NOT_CONFIRMED' },
    })
    await expect(service.create(DEMO_USER.id, { sourceKey: 'videos/ausente.mp4' })).resolves.toMatchObject({
      error: { code: 'VIDEO_NOT_FOUND' },
    })
  })

  it('replays the same job for an idempotency key and conflicts on a different payload', async () => {
    const service = new MockJobService()
    const first = await service.create(DEMO_USER.id, {
      sourceKey: CONFIRMED_SOURCE_KEY,
      idempotencyKey: 'create-1',
    })
    const replay = await service.create(DEMO_USER.id, {
      sourceKey: CONFIRMED_SOURCE_KEY,
      idempotencyKey: 'create-1',
    })

    expect('job' in first && 'job' in replay && first.job.id === replay.job.id).toBe(true)
    await expect(
      service.create(DEMO_USER.id, { sourceKey: 'videos/outro.mp4', idempotencyKey: 'create-1' }),
    ).resolves.toMatchObject({ error: { code: 'IDEMPOTENCY_CONFLICT' } })
  })
})
