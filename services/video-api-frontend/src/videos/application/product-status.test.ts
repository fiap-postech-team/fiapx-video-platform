import { describe, expect, it } from 'vitest'
import { copy } from '../../product-copy'
import { demoVideos } from '../infrastructure/fixtures'
import { lifecycleStatusLabel, videoMilestones } from './product-status'

function videoByRef(videoRef: string) {
  const video = demoVideos().find((item) => item.item.videoRef === videoRef)
  if (!video) {
    throw new Error(videoRef)
  }
  return video
}

describe('product status', () => {
  it('maps each library status to the owner-facing label', () => {
    expect(lifecycleStatusLabel(videoByRef('ref-pending').item.status)).toBe(copy.lifecycleStatus.pending)
    expect(lifecycleStatusLabel(videoByRef('ref-uploaded').item.status)).toBe(copy.lifecycleStatus.processing)
    expect(lifecycleStatusLabel(videoByRef('ref-queued').item.status)).toBe(copy.lifecycleStatus.processing)
    expect(lifecycleStatusLabel(videoByRef('ref-available').item.status)).toBe(copy.lifecycleStatus.available)
    expect(lifecycleStatusLabel(videoByRef('ref-failed').item.status)).toBe(copy.lifecycleStatus.failed)
    expect(lifecycleStatusLabel(videoByRef('ref-rejected').item.status)).toBe(copy.lifecycleStatus.rejected)
    expect(lifecycleStatusLabel(videoByRef('ref-expired').item.status)).toBe(copy.lifecycleStatus.expired)
  })
})

describe('videoMilestones', () => {
  it('shows awaiting for future milestones and unavailable for missing historical data', () => {
    expect(videoMilestones(videoByRef('ref-pending').detail)).toEqual({
      sent: { kind: 'awaiting' },
      processed: { kind: 'awaiting' },
      available: { kind: 'awaiting' },
    })
    expect(videoMilestones(videoByRef('ref-uploaded').detail).processed).toEqual({ kind: 'awaiting' })
    expect(videoMilestones(videoByRef('ref-failed').detail)).toEqual({
      sent: { kind: 'date', iso: '2026-09-15T12:01:00Z' },
      processed: { kind: 'unavailable' },
      available: { kind: 'unavailable' },
    })
    expect(videoMilestones(videoByRef('ref-expired').detail)).toEqual({
      sent: { kind: 'unavailable' },
      processed: { kind: 'unavailable' },
      available: { kind: 'unavailable' },
    })
  })
})
