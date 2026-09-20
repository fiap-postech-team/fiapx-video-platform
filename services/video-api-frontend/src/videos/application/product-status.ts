import { copy } from '../../product-copy'
import type {
  LifecycleStatusKey,
  ProcessingAttempt,
  ProcessingStatusKey,
  Video,
  VideoStatusKey,
} from '../domain/video'

export type MilestoneKind = 'date' | 'awaiting' | 'unavailable'

export interface Milestone {
  kind: MilestoneKind
  iso?: string
}

export function compareAttempts(left: ProcessingAttempt, right: ProcessingAttempt): number {
  return right.createdAt.localeCompare(left.createdAt) || right.id.localeCompare(left.id)
}

export function latestAttempt(video: Video): ProcessingAttempt | null {
  if (video.attempts.length === 0) {
    return null
  }
  return [...video.attempts].sort(compareAttempts)[0] ?? null
}

export function previousAttempts(video: Video): ProcessingAttempt[] {
  const latest = latestAttempt(video)
  return video.attempts
    .filter((attempt) => attempt.id !== latest?.id)
    .sort(compareAttempts)
}

export function videoStatus(video: Video): VideoStatusKey {
  switch (video.uploadStatus) {
    case 'PENDING':
      return 'pending'
    case 'UPLOADED':
      return 'uploaded'
    case 'REJECTED':
      return 'rejected'
    case 'EXPIRED':
      return 'expired'
  }
}

export function processingStatus(video: Video): ProcessingStatusKey {
  const attempt = latestAttempt(video)
  if (!attempt) {
    return 'pending'
  }
  switch (attempt.status) {
    case 'PENDING':
      return 'pending'
    case 'PROCESSING':
      return 'processing'
    case 'COMPLETED':
      return 'completed'
    case 'FAILED':
      return 'error'
  }
}

export function videoStatusLabel(status: VideoStatusKey): string {
  return copy.videoStatus[status]
}

export function processingStatusLabel(status: ProcessingStatusKey): string {
  return copy.processingStatus[status]
}

export function lifecycleStatus(video: Video): LifecycleStatusKey {
  switch (video.uploadStatus) {
    case 'PENDING':
      return 'pending'
    case 'REJECTED':
      return 'rejected'
    case 'EXPIRED':
      return 'expired'
    case 'UPLOADED': {
      const attempt = latestAttempt(video)
      if (attempt?.status === 'COMPLETED') {
        return 'available'
      }
      if (attempt?.status === 'FAILED') {
        return 'failed'
      }
      return 'processing'
    }
  }
}

export function lifecycleStatusLabel(status: LifecycleStatusKey): string {
  return copy.lifecycleStatus[status]
}

export function videoMilestones(video: Video): {
  sent: Milestone
  processed: Milestone
  available: Milestone
} {
  const upload = videoStatus(video)
  const processing = processingStatus(video)
  const attempt = latestAttempt(video)

  const sent: Milestone = video.uploadedAt
    ? { kind: 'date', iso: video.uploadedAt }
    : upload === 'pending'
      ? { kind: 'awaiting' }
      : { kind: 'unavailable' }

  const processed: Milestone = attempt?.processedAt
    ? { kind: 'date', iso: attempt.processedAt }
    : processing === 'error' || processing === 'completed' || upload === 'expired' || upload === 'rejected'
      ? { kind: 'unavailable' }
      : { kind: 'awaiting' }

  const available: Milestone = attempt?.availableAt
    ? { kind: 'date', iso: attempt.availableAt }
    : processing === 'error' || processing === 'completed' || upload === 'expired' || upload === 'rejected'
      ? { kind: 'unavailable' }
      : { kind: 'awaiting' }

  return { sent, processed, available }
}

export function compareVideos(left: Video, right: Video): number {
  return right.requestedAt.localeCompare(left.requestedAt) || right.id.localeCompare(left.id)
}
