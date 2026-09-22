import { useEffect, useRef, useState } from 'react'
import { copy } from '../../product-copy'
import { lifecycleStatusLabel, lifecycleTone } from '../application/product-status'
import { isVideoServiceError, type VideoDetail as VideoDetailModel, type VideoService } from '../domain/video'
import { VideoTimeline } from './VideoTimeline'

interface VideoDetailProps {
  videoRef: string
  videoService: VideoService
  onBack: () => void
}

const DETAIL_POLL_INTERVAL_MS = 15_000

export function VideoDetail({ videoRef, videoService, onBack }: VideoDetailProps) {
  const [video, setVideo] = useState<VideoDetailModel | null | undefined>(undefined)
  const [failed, setFailed] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const shownRef = useRef<string | null>(null)

  useEffect(() => {
    let cancelled = false
    let pollTimer: ReturnType<typeof setTimeout> | undefined
    if (shownRef.current !== videoRef) {
      setVideo(undefined)
      setFailed(false)
    }
    videoService.get(videoRef).then((result) => {
      if (cancelled) return
      shownRef.current = videoRef
      setFailed(false)
      setVideo(result)
      if (result && (result.status === 'UPLOADED' || result.status === 'PROCESSING')) {
        pollTimer = setTimeout(() => setReloadKey((value) => value + 1), DETAIL_POLL_INTERVAL_MS)
      }
    }).catch((error) => {
      if (cancelled) return
      if (isVideoServiceError(error)) {
        setFailed(true)
        return
      }
      setVideo(null)
    })
    return () => {
      cancelled = true
      if (pollTimer) clearTimeout(pollTimer)
    }
  }, [videoRef, videoService, reloadKey])

  if (video === undefined && !failed) {
    return (
      <section className="page-block">
        <p role="status">{copy.videos.loading}</p>
      </section>
    )
  }

  if (failed) {
    return (
      <section className="page-block">
        <p role="alert">{copy.detail.unavailableLoad}</p>
        <button type="button" className="btn-primary" onClick={() => setReloadKey((value) => value + 1)}>
          {copy.videos.retry}
        </button>
      </section>
    )
  }

  if (!video) {
    return (
      <section className="page-block">
        <p role="alert">{copy.detail.notFound}</p>
        <div className="detail-actions">
          <button type="button" onClick={onBack}>{copy.detail.back}</button>
        </div>
      </section>
    )
  }

  const tone = lifecycleTone(video.status)

  return (
    <article className="page-block" aria-labelledby="video-detail-title">
      <div className="detail-header">
        <p className={`status-badge is-${tone}`}>
          {lifecycleStatusLabel(video.status)}
        </p>
        <h1 id="video-detail-title">{video.originalFilename}</h1>
      </div>
      <VideoTimeline video={video} />
      {video.status === 'EXPIRED' && <p className="page-note">{copy.detail.expiredHint}</p>}
      {video.status === 'FAILED' && <p className="page-note">{copy.detail.failedHint}</p>}
      <div className="detail-actions">
        <button type="button" onClick={onBack}>{copy.detail.back}</button>
      </div>
    </article>
  )
}
