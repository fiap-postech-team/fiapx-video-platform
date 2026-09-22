import { copy } from '../../product-copy'
import type {
  LifecycleTone,
  ProductProcessingStatus,
  ProductVideoStatus,
  VideoDetail,
} from '../domain/video'

export type MilestoneKind = 'date' | 'awaiting' | 'unavailable'

export interface Milestone {
  kind: MilestoneKind
  iso?: string
}

const TONE_BY_STATUS: Record<ProductVideoStatus, LifecycleTone> = {
  AWAITING_UPLOAD: 'pending',
  UPLOADED: 'processing',
  PROCESSING: 'processing',
  AVAILABLE: 'available',
  FAILED: 'failed',
  REJECTED: 'rejected',
  EXPIRED: 'expired',
}

export function lifecycleTone(status: ProductVideoStatus): LifecycleTone {
  return TONE_BY_STATUS[status]
}

export function lifecycleStatusLabel(status: ProductVideoStatus): string {
  return copy.lifecycleStatus[TONE_BY_STATUS[status]]
}

export function processingStatusLabel(status: ProductProcessingStatus): string {
  switch (status) {
    case 'QUEUED':
    case 'PROCESSING':
      return copy.processingStatus.processing
    case 'AVAILABLE':
      return copy.processingStatus.completed
    case 'FAILED':
      return copy.processingStatus.error
  }
}

export function videoMilestones(detail: VideoDetail): {
  sent: Milestone
  processed: Milestone
  available: Milestone
} {
  const sent: Milestone = detail.uploadedAt
    ? { kind: 'date', iso: detail.uploadedAt }
    : detail.status === 'AWAITING_UPLOAD'
      ? { kind: 'awaiting' }
      : { kind: 'unavailable' }

  const processing = detail.processing
  const terminal = detail.status === 'FAILED' || detail.status === 'REJECTED' || detail.status === 'EXPIRED'
  const processed: Milestone = processing?.startedAt
    ? { kind: 'date', iso: processing.startedAt }
    : terminal
      ? { kind: 'unavailable' }
      : { kind: 'awaiting' }

  const available: Milestone = processing?.status === 'AVAILABLE' && processing.finishedAt
    ? { kind: 'date', iso: processing.finishedAt }
    : terminal
      ? { kind: 'unavailable' }
      : { kind: 'awaiting' }

  return { sent, processed, available }
}
