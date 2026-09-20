import { useEffect, useState } from 'react'
import { copy, interpolate } from '../../product-copy'
import { formatDateTime } from '../application/format-datetime'
import {
  videoStatus,
  videoStatusLabel,
} from '../application/product-status'
import type { PrototypeScenario, Video, VideoService } from '../domain/video'

interface VideoListProps {
  userId: string
  videoService: VideoService
  scenario: PrototypeScenario
  onOpen: (videoId: string) => void
  onUpload: () => void
}

const PAGE_SIZE = 20

export function VideoList({ userId, videoService, scenario, onOpen, onUpload }: VideoListProps) {
  const [videos, setVideos] = useState<Video[]>([])
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isLoadingMore, setIsLoadingMore] = useState(false)
  const [failed, setFailed] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    setIsLoading(true)
    setFailed(false)
    setVideos([])
    setNextCursor(null)

    if (scenario.kind === 'loading') {
      return () => {
        cancelled = true
      }
    }

    videoService
      .list(userId, { limit: PAGE_SIZE, scenario })
      .then((page) => {
        if (cancelled) return
        setVideos(page.items)
        setNextCursor(page.nextCursor)
      })
      .catch(() => {
        if (cancelled) return
        setFailed(true)
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [userId, videoService, scenario, reloadKey])

  async function loadMore() {
    if (!nextCursor || isLoadingMore) return
    setIsLoadingMore(true)
    try {
      const page = await videoService.list(userId, { cursor: nextCursor, limit: PAGE_SIZE, scenario })
      setVideos((current) => [...current, ...page.items])
      setNextCursor(page.nextCursor)
    } catch {
      setFailed(true)
    } finally {
      setIsLoadingMore(false)
    }
  }

  return (
    <section className="page-block" aria-labelledby="videos-title">
      <h1 id="videos-title">{copy.videos.title}</h1>
      <p className="page-lead">{copy.videos.lead}</p>

      {isLoading || scenario.kind === 'loading' ? (
        <p className="empty-state" role="status">{copy.videos.loading}</p>
      ) : failed ? (
        <div className="empty-state" role="alert">
          <p>{copy.videos.error}</p>
          <button type="button" onClick={() => setReloadKey((value) => value + 1)}>
            {copy.videos.retry}
          </button>
        </div>
      ) : videos.length === 0 ? (
        <div className="empty-state">
          <h2>{copy.videos.emptyTitle}</h2>
          <p>{copy.videos.emptyBody}</p>
          <button type="button" onClick={onUpload}>{copy.videos.emptyAction}</button>
        </div>
      ) : (
        <ul className="video-list">
          {videos.map((video) => {
            const upload = videoStatus(video)
            return (
              <li key={video.id}>
                <article className="video-row">
                  <span className="file-mark" aria-hidden="true" />
                  <div>
                    <p className={`status-badge is-${upload}`}>
                      <span>{copy.status.videoLabel}</span>
                      <strong>{videoStatusLabel(upload)}</strong>
                    </p>
                    <h2>{video.originalFilename}</h2>
                    {video.uploadedAt && (
                      <p className="video-meta">
                        {interpolate(copy.videos.sentAt, { data: formatDateTime(video.uploadedAt) })}
                      </p>
                    )}
                  </div>
                  <button type="button" className="ghost compact" onClick={() => onOpen(video.id)}>
                    {copy.videos.openDetail}
                  </button>
                </article>
              </li>
            )
          })}
        </ul>
      )}

      {nextCursor && !failed && !isLoading && (
        <button type="button" className="ghost" onClick={() => { void loadMore() }} disabled={isLoadingMore}>
          {isLoadingMore ? copy.videos.loadMorePending : copy.videos.loadMore}
        </button>
      )}
    </section>
  )
}
