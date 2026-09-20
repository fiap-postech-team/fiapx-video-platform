import { describe, expect, it } from 'vitest'
import { COPY_INVENTORY, copy, findForbiddenTerms } from './product-copy'
import { lifecycleStatus, lifecycleStatusLabel } from './videos/application/product-status'
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

  it('maps each fixture to one owner-facing lifecycle label', () => {
    const labels = demoVideos().map((video) => lifecycleStatusLabel(lifecycleStatus(video)))
    expect(labels).toEqual([
      copy.lifecycleStatus.pending,
      copy.lifecycleStatus.processing,
      copy.lifecycleStatus.processing,
      copy.lifecycleStatus.processing,
      copy.lifecycleStatus.available,
      copy.lifecycleStatus.failed,
      copy.lifecycleStatus.rejected,
      copy.lifecycleStatus.expired,
    ])
  })
})
