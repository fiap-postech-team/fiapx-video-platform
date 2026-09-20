import { copy } from '../../product-copy'
import type { Milestone } from './product-status'

const DATE_FORMAT: Intl.DateTimeFormatOptions = { dateStyle: 'short', timeStyle: 'short' }

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat('pt-BR', DATE_FORMAT).format(new Date(iso))
}

export function formatMilestone(milestone: Milestone): string {
  if (milestone.kind === 'date' && milestone.iso) {
    return formatDateTime(milestone.iso)
  }
  if (milestone.kind === 'awaiting') {
    return copy.detail.awaiting
  }
  return copy.detail.unavailable
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1_000_000) {
    const kilobytes = bytes / 1_000
    return `${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(kilobytes)} KB`
  }
  const megabytes = bytes / 1_000_000
  return `${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(megabytes)} MB`
}
