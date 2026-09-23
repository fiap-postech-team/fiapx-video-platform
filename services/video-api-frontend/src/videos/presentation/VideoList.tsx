import { useEffect, useState } from 'react'
import { copy, interpolate } from '../../product-copy'
import { formatDateTime } from '../application/format-datetime'
import { lifecycleStatusLabel, lifecycleTone } from '../application/product-status'
import type { PrototypeScenario, VideoLibraryItem, VideoService } from '../domain/video'
import { DownloadResultButton } from './DownloadResultButton'

interface VideoListProps {
  videoService: VideoService
  scenario: PrototypeScenario
  onOpen: (videoRef: string) => void
  onUpload: () => void
}

export function VideoList({ videoService, scenario, onOpen, onUpload }: VideoListProps) {
  const [items, setItems] = useState<VideoLibraryItem[]>([])
  const [page, setPage] = useState(1)
  const [totalItems, setTotalItems] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [failed, setFailed] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    setIsLoading(true)
    setFailed(false)

    if (scenario.kind === 'loading') {
      return () => {
        cancelled = true
      }
    }

    videoService
      .list(page, { scenario })
      .then((result) => {
        if (cancelled) return
        setItems(result.items)
        setTotalItems(result.totalItems)
        setTotalPages(result.totalPages)
      })
      .catch(() => {
        if (!cancelled) setFailed(true)
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [videoService, scenario, page, reloadKey])

  return (
    <section className="page-block" aria-labelledby="videos-title">
      <header className="page-header">
        <div>
          <h1 id="videos-title">{copy.videos.title}</h1>
          <p className="page-lead">{copy.videos.lead}</p>
        </div>
        <button type="button" className="btn-primary" onClick={onUpload}>
          {copy.shell.navUpload}
        </button>
      </header>

      {isLoading || scenario.kind === 'loading' ? (
        <p className="empty-state" role="status">{copy.videos.loading}</p>
      ) : failed ? (
        <div className="empty-state" role="alert">
          <p>{copy.videos.error}</p>
          <button type="button" className="btn-primary" onClick={() => setReloadKey((value) => value + 1)}>
            {copy.videos.retry}
          </button>
        </div>
      ) : totalItems === 0 ? (
        <div className="empty-state">
          <h2>{copy.videos.emptyTitle}</h2>
          <p>{copy.videos.emptyBody}</p>
          <button type="button" className="btn-primary" onClick={onUpload}>{copy.videos.emptyAction}</button>
        </div>
      ) : (
        <>
          <div className="video-table" role="table" aria-label={copy.videos.title}>
            <div className="video-head" role="row">
              <span role="columnheader">{copy.videos.colFile}</span>
              <span role="columnheader">{copy.videos.colDate}</span>
              <span role="columnheader">{copy.videos.colStatus}</span>
              <span role="columnheader" className="video-head-action">{copy.videos.colActions}</span>
            </div>
            {items.map((video) => {
              const tone = lifecycleTone(video.status)
              return (
                <div key={video.videoRef} className="video-row" role="row">
                  <span className="file-name" role="cell" data-label={copy.videos.colFile}>{video.originalFilename}</span>
                  <span className="file-date" role="cell" data-label={copy.videos.colDate}>{formatDateTime(video.activityAt)}</span>
                  <span className="status-cell" role="cell" data-label={copy.videos.colStatus}>
                    <span className={`status-badge is-${tone}`}>{lifecycleStatusLabel(video.status)}</span>
                  </span>
                  <span role="cell" className="row-action" data-label={copy.videos.colActions}>
                    <button type="button" className="btn-quiet" onClick={() => onOpen(video.videoRef)}>
                      {copy.videos.openDetail}
                    </button>
                    {video.status === 'AVAILABLE' && video.jobId && (
                      <DownloadResultButton
                        jobId={video.jobId}
                        videoService={videoService}
                        className="btn-icon"
                        iconOnly
                      />
                    )}
                  </span>
                </div>
              )
            })}
          </div>
          {totalPages > 1 && (
            <nav className="page-nav" aria-label={copy.videos.pagination}>
              <button
                type="button"
                className="btn-quiet"
                disabled={page <= 1}
                onClick={() => setPage((current) => current - 1)}
              >
                {copy.videos.pagePrevious}
              </button>
              {Array.from({ length: totalPages }, (_, index) => index + 1).map((number) => (
                <button
                  key={number}
                  type="button"
                  className={number === page ? 'page-number is-current' : 'page-number'}
                  aria-current={number === page ? 'page' : undefined}
                  aria-label={interpolate(copy.videos.pageLabel, { n: number })}
                  onClick={() => setPage(number)}
                >
                  {number}
                </button>
              ))}
              <button
                type="button"
                className="btn-quiet"
                disabled={page >= totalPages}
                onClick={() => setPage((current) => current + 1)}
              >
                {copy.videos.pageNext}
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}
