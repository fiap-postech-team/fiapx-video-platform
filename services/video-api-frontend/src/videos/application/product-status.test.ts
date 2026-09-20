import { describe, expect, it } from 'vitest'
import { copy } from '../../product-copy'
import type { Video } from '../domain/video'
import { demoVideos } from '../infrastructure/fixtures'
import {
  latestAttempt,
  lifecycleStatus,
  lifecycleStatusLabel,
  previousAttempts,
  processingStatus,
  processingStatusLabel,
  videoMilestones,
} from './product-status'

function videoById(id: string): Video {
  const video = demoVideos().find((item) => item.id === id)
  if (!video) {
    throw new Error(id)
  }
  return video
}

describe('video and processing status', () => {
  it('derives one owner-facing lifecycle status from upload and latest attempt', () => {
    expect(lifecycleStatusLabel(lifecycleStatus(videoById('video-pending')))).toBe(copy.lifecycleStatus.pending)
    expect(lifecycleStatusLabel(lifecycleStatus(videoById('video-uploaded')))).toBe(copy.lifecycleStatus.processing)
    expect(lifecycleStatusLabel(lifecycleStatus(videoById('video-queued')))).toBe(copy.lifecycleStatus.processing)
    expect(lifecycleStatusLabel(lifecycleStatus(videoById('video-processing')))).toBe(copy.lifecycleStatus.processing)
    expect(lifecycleStatusLabel(lifecycleStatus(videoById('video-available')))).toBe(copy.lifecycleStatus.available)
    expect(lifecycleStatusLabel(lifecycleStatus(videoById('video-failed')))).toBe(copy.lifecycleStatus.failed)
    expect(lifecycleStatusLabel(lifecycleStatus(videoById('video-rejected')))).toBe(copy.lifecycleStatus.rejected)
    expect(lifecycleStatusLabel(lifecycleStatus(videoById('video-expired')))).toBe(copy.lifecycleStatus.expired)
    expect(processingStatusLabel(processingStatus(videoById('video-available')))).toBe(copy.processingStatus.completed)
  })

  it('keeps a single latest attempt for a video with history', () => {
    const video = videoById('video-available')
    expect(latestAttempt(video)?.id).toBe('attempt-available-done')
    expect(previousAttempts(video).map((attempt) => attempt.id)).toEqual(['attempt-available-failed'])
  })
})

describe('videoMilestones', () => {
  it('shows awaiting for future milestones and unavailable for missing historical data', () => {
    expect(videoMilestones(videoById('video-pending'))).toEqual({
      sent: { kind: 'awaiting' },
      processed: { kind: 'awaiting' },
      available: { kind: 'awaiting' },
    })
    expect(videoMilestones(videoById('video-uploaded')).processed).toEqual({ kind: 'awaiting' })
    expect(videoMilestones(videoById('video-failed'))).toEqual({
      sent: { kind: 'date', iso: '2026-09-15T12:01:00Z' },
      processed: { kind: 'unavailable' },
      available: { kind: 'unavailable' },
    })
    expect(videoMilestones(videoById('video-expired'))).toEqual({
      sent: { kind: 'unavailable' },
      processed: { kind: 'unavailable' },
      available: { kind: 'unavailable' },
    })
  })
})
