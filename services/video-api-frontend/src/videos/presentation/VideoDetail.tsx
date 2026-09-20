import { useEffect, useState } from 'react'
import { copy } from '../../product-copy'
import { formatDateTime } from '../application/format-datetime'
import {
  previousAttempts,
  processingStatus,
  processingStatusLabel,
  videoStatus,
} from '../application/product-status'
import type { Video, VideoService } from '../domain/video'
import { VideoTimeline } from './VideoTimeline'

interface VideoDetailProps {
  userId: string
  videoId: string
  videoService: VideoService
  onBack: () => void
}

export function VideoDetail({ userId, videoId, videoService, onBack }: VideoDetailProps) {
  const [video, setVideo] = useState<Video | null | undefined>(undefined)
  const [downloadNotice, setDownloadNotice] = useState(false)

  useEffect(() => {
    let cancelled = false
    setVideo(undefined)
    setDownloadNotice(false)
    videoService.get(userId, videoId).then((result) => {
      if (!cancelled) setVideo(result)
    })
    return () => {
      cancelled = true
    }
  }, [userId, videoId, videoService])

  if (video === undefined) {
    return (
      <section className="page-block">
        <p role="status">{copy.videos.loading}</p>
      </section>
    )
  }

  if (video === null) {
    return (
      <section className="page-block">
        <p role="alert">{copy.detail.notFound}</p>
        <button type="button" className="ghost" onClick={onBack}>{copy.detail.back}</button>
      </section>
    )
  }

  const upload = videoStatus(video)
  const processing = processingStatus(video)
  const history = previousAttempts(video)

  return (
    <article className="page-block" aria-labelledby="video-detail-title">
      <div className="detail-header">
        <button type="button" className="text-link" onClick={onBack}>{copy.detail.back}</button>
        <p className={`status-badge is-proc-${processing}`}>
          <span>{copy.status.processingLabel}</span>
          <strong>{processingStatusLabel(processing)}</strong>
        </p>
        <h1 id="video-detail-title">{video.originalFilename}</h1>
      </div>
      <VideoTimeline video={video} />

      {processing === 'completed' && (
        <div className="detail-actions">
          <button type="button" onClick={() => setDownloadNotice(true)}>
            {copy.detail.download}
          </button>
          {downloadNotice && (
            <p className="notice" role="status">
              {copy.detail.downloadDone} {copy.detail.downloadSimulated}
            </p>
          )}
        </div>
      )}
      {upload === 'expired' && <p className="page-note">{copy.detail.expiredHint}</p>}
      {processing === 'error' && <p className="page-note">{copy.detail.failedHint}</p>}

      <section className="history" aria-labelledby="history-title">
        <h2 id="history-title">{copy.detail.history}</h2>
        {history.length === 0 ? (
          <p>{copy.detail.historyEmpty}</p>
        ) : (
          <ol>
            {history.map((attempt) => (
              <li key={attempt.id}>
                <strong>{processingStatusLabel(
                  attempt.status === 'COMPLETED'
                    ? 'completed'
                    : attempt.status === 'PROCESSING'
                      ? 'processing'
                      : attempt.status === 'PENDING'
                        ? 'pending'
                        : 'error',
                )}</strong>
                <span>{formatDateTime(attempt.createdAt)}</span>
              </li>
            ))}
          </ol>
        )}
      </section>
    </article>
  )
}
