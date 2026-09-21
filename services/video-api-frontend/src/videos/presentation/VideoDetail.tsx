import { useEffect, useState } from 'react'
import { copy } from '../../product-copy'
import { lifecycleStatusLabel, lifecycleTone } from '../application/product-status'
import { isVideoServiceError, type VideoDetail as VideoDetailModel, type VideoService } from '../domain/video'
import { VideoTimeline } from './VideoTimeline'

interface VideoDetailProps {
  videoRef: string
  videoService: VideoService
  onBack: () => void
}

export function VideoDetail({ videoRef, videoService, onBack }: VideoDetailProps) {
  const [video, setVideo] = useState<VideoDetailModel | null | undefined>(undefined)
  const [failed, setFailed] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    setVideo(undefined)
    setFailed(false)
    videoService.get(videoRef).then((result) => {
      if (!cancelled) setVideo(result)
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

  if (video === null) {
    return (
      <section className="page-block">
        <p role="alert">{copy.detail.notFound}</p>
        <button type="button" className="btn-quiet" onClick={onBack}>{copy.detail.back}</button>
      </section>
    )
  }

  const tone = lifecycleTone(video.status)

  return (
    <article className="page-block" aria-labelledby="video-detail-title">
      <div className="detail-header">
        <button type="button" className="text-link" onClick={onBack}>{copy.detail.back}</button>
        <p className={`status-badge is-${tone}`}>
          {lifecycleStatusLabel(video.status)}
        </p>
        <h1 id="video-detail-title">{video.originalFilename}</h1>
      </div>
      <VideoTimeline video={video} />
      {video.status === 'EXPIRED' && <p className="page-note">{copy.detail.expiredHint}</p>}
      {video.status === 'FAILED' && <p className="page-note">{copy.detail.failedHint}</p>}
    </article>
  )
}
