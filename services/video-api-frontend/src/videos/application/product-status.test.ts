import { describe, expect, it } from 'vitest'
import { copy } from '../../product-copy'
import type { Video } from '../domain/video'
import { demoVideos } from '../infrastructure/fixtures'
import {
  latestAttempt,
  previousAttempts,
  processingStatus,
  processingStatusLabel,
  videoMilestones,
  videoStatus,
  videoStatusLabel,
} from './product-status'

function videoById(id: string): Video {
  const video = demoVideos().find((item) => item.id === id)
  if (!video) {
    throw new Error(id)
  }
  return video
}

describe('video and processing status', () => {
  it('exposes only the allowed video and processing labels', () => {
    expect(videoStatusLabel(videoStatus(videoById('video-pending')))).toBe(copy.videoStatus.pending)
    expect(videoStatusLabel(videoStatus(videoById('video-uploaded')))).toBe(copy.videoStatus.uploaded)
    expect(videoStatusLabel(videoStatus(videoById('video-rejected')))).toBe(copy.videoStatus.rejected)
    expect(videoStatusLabel(videoStatus(videoById('video-expired')))).toBe(copy.videoStatus.expired)
    expect(processingStatusLabel(processingStatus(videoById('video-queued')))).toBe(copy.processingStatus.pending)
    expect(processingStatusLabel(processingStatus(videoById('video-processing')))).toBe(copy.processingStatus.processing)
    expect(processingStatusLabel(processingStatus(videoById('video-available')))).toBe(copy.processingStatus.completed)
    expect(processingStatusLabel(processingStatus(videoById('video-failed')))).toBe(copy.processingStatus.error)
    expect(processingStatusLabel(processingStatus(videoById('video-uploaded')))).toBe(copy.processingStatus.pending)
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
