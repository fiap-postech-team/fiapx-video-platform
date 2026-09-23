import type { VideoDetail, VideoLibraryItem } from '../domain/video'

export interface DemoVideo {
  item: VideoLibraryItem
  detail: VideoDetail
}

export function demoVideos(): DemoVideo[] {
  return [
    library(
      'ref-pending',
      'aula-gravada.mp4',
      'AWAITING_UPLOAD',
      '2026-09-19T18:10:00Z',
      '2026-09-19T18:10:00Z',
      null,
      null,
    ),
    library(
      'ref-uploaded',
      'apresentacao.mp4',
      'UPLOADED',
      '2026-09-18T13:40:00Z',
      '2026-09-18T13:42:00Z',
      '2026-09-18T13:42:00Z',
      null,
    ),
    library(
      'ref-queued',
      'reuniao.webm',
      'PROCESSING',
      '2026-09-18T11:00:00Z',
      '2026-09-18T11:03:00Z',
      '2026-09-18T11:02:00Z',
      { jobId: 'job-ref-queued', status: 'QUEUED', requestedAt: '2026-09-18T11:03:00Z', startedAt: null, finishedAt: null },
    ),
    library(
      'ref-processing',
      'treino.mov',
      'PROCESSING',
      '2026-09-17T16:20:00Z',
      '2026-09-17T16:22:00Z',
      '2026-09-17T16:21:00Z',
      {
        jobId: 'job-ref-processing',
        status: 'PROCESSING',
        requestedAt: '2026-09-17T16:22:00Z',
        startedAt: '2026-09-17T16:22:30Z',
        finishedAt: null,
      },
    ),
    library(
      'ref-available',
      'campanha.mp4',
      'AVAILABLE',
      '2026-09-16T14:00:00Z',
      '2026-09-16T14:10:00Z',
      '2026-09-16T14:02:00Z',
      {
        jobId: 'job-ref-available',
        status: 'AVAILABLE',
        requestedAt: '2026-09-16T14:03:00Z',
        startedAt: '2026-09-16T14:04:00Z',
        finishedAt: '2026-09-16T14:10:00Z',
      },
    ),
    library(
      'ref-failed',
      'entrevista.mp4',
      'FAILED',
      '2026-09-15T12:00:00Z',
      '2026-09-15T12:02:00Z',
      '2026-09-15T12:01:00Z',
      {
        jobId: 'job-ref-failed',
        status: 'FAILED',
        requestedAt: '2026-09-15T12:02:00Z',
        startedAt: null,
        finishedAt: '2026-09-15T12:08:00Z',
      },
    ),
    library(
      'ref-rejected',
      'material.mp4',
      'REJECTED',
      '2026-09-14T09:30:00Z',
      '2026-09-14T09:30:00Z',
      '2026-09-14T09:31:00Z',
      null,
    ),
    library(
      'ref-expired',
      'rascunho.mp4',
      'EXPIRED',
      '2026-09-10T08:00:00Z',
      '2026-09-10T08:00:00Z',
      null,
      null,
    ),
  ]
}

function library(
  videoRef: string,
  originalFilename: string,
  status: VideoLibraryItem['status'],
  submittedAt: string,
  activityAt: string,
  uploadedAt: string | null,
  processing: VideoDetail['processing'],
): DemoVideo {
  const jobId = processing ? `job-${videoRef}` : ''
  const normalizedProcessing = processing ? { ...processing, jobId } : null
  const item: VideoLibraryItem = { videoRef, originalFilename, status, jobId: jobId || null, submittedAt, activityAt }
  return { item, detail: { ...item, uploadedAt, processing: normalizedProcessing } }
}
