import { describe, expect, it } from 'vitest'
import { copy } from '../../product-copy'
import { formatDateTime, formatFileSize, formatMilestone } from './format-datetime'

describe('formatDateTime', () => {
  it('formats a UTC instant in the test timezone', () => {
    expect(formatDateTime('2026-09-18T13:42:00Z')).toBe(
      new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(
        new Date('2026-09-18T13:42:00Z'),
      ),
    )
  })
})

describe('formatMilestone', () => {
  it('uses product labels for missing and future timestamps', () => {
    expect(formatMilestone({ kind: 'awaiting' })).toBe(copy.detail.awaiting)
    expect(formatMilestone({ kind: 'unavailable' })).toBe(copy.detail.unavailable)
    expect(formatMilestone({ kind: 'date', iso: '2026-09-18T13:42:00Z' })).toBe(formatDateTime('2026-09-18T13:42:00Z'))
  })
})

describe('formatFileSize', () => {
  it('formats megabytes in pt-BR', () => {
    expect(formatFileSize(125_829_120)).toContain('MB')
  })
})
