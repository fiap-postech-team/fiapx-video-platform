import { describe, expect, it } from 'vitest'
import { COPY_INVENTORY, copy, findForbiddenTerms } from './product-copy'
import { processingStatus, processingStatusLabel, videoStatus, videoStatusLabel } from './videos/application/product-status'
import { demoVideos } from './videos/infrastructure/fixtures'

describe('product copy inventory', () => {
  it('documents every catalog key with a screen and condition', () => {
    const keys = COPY_INVENTORY.map((entry) => entry.key)
    expect(new Set(keys).size).toBe(keys.length)
    for (const entry of COPY_INVENTORY) {
      expect(entry.screen.length).toBeGreaterThan(0)
      expect(entry.condition.length).toBeGreaterThan(0)
      expect(entry.textOrPattern.length).toBeGreaterThan(0)
    }
  })

  it('keeps user-facing catalog text free of internal terms', () => {
    for (const entry of COPY_INVENTORY) {
      expect(findForbiddenTerms(entry.textOrPattern), entry.key).toEqual([])
    }
  })

  it('maps each fixture to the allowed Portuguese status labels', () => {
    const labels = demoVideos().map((video) => [
      videoStatusLabel(videoStatus(video)),
      processingStatusLabel(processingStatus(video)),
    ])
    expect(labels).toEqual([
      [copy.videoStatus.pending, copy.processingStatus.pending],
      [copy.videoStatus.uploaded, copy.processingStatus.pending],
      [copy.videoStatus.uploaded, copy.processingStatus.pending],
      [copy.videoStatus.uploaded, copy.processingStatus.processing],
      [copy.videoStatus.uploaded, copy.processingStatus.completed],
      [copy.videoStatus.uploaded, copy.processingStatus.error],
      [copy.videoStatus.rejected, copy.processingStatus.pending],
      [copy.videoStatus.expired, copy.processingStatus.pending],
    ])
  })
})
