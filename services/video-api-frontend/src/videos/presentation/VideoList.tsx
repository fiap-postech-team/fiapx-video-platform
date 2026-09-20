import { useEffect, useMemo, useState } from 'react'
import { copy, interpolate } from '../../product-copy'
import { formatDateTime } from '../application/format-datetime'
import { lifecycleStatus, lifecycleStatusLabel } from '../application/product-status'
import { LIFECYCLE_STATUSES, type LifecycleStatusKey, type PrototypeScenario, type Video, type VideoService } from '../domain/video'

interface VideoListProps {
  userId: string
  videoService: VideoService
  scenario: PrototypeScenario
  onOpen: (videoId: string) => void
  onUpload: () => void
}

const PAGE_SIZE = 20
const STATUS_FILTERS: Array<'all' | LifecycleStatusKey> = ['all', ...LIFECYCLE_STATUSES]

export function VideoList({ userId, videoService, scenario, onOpen, onUpload }: VideoListProps) {
  const [videos, setVideos] = useState<Video[]>([])
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isLoadingMore, setIsLoadingMore] = useState(false)
  const [failed, setFailed] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<'all' | LifecycleStatusKey>('all')

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

  const visible = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return videos.filter((video) => {
      const status = lifecycleStatus(video)
      if (statusFilter !== 'all' && status !== statusFilter) return false
      if (needle && !video.originalFilename.toLowerCase().includes(needle)) return false
      return true
    })
  }, [videos, query, statusFilter])

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
      ) : videos.length === 0 ? (
        <div className="empty-state">
          <h2>{copy.videos.emptyTitle}</h2>
          <p>{copy.videos.emptyBody}</p>
          <button type="button" className="btn-primary" onClick={onUpload}>{copy.videos.emptyAction}</button>
        </div>
      ) : (
        <>
          <div className="list-toolbar">
            <p className="list-count">{interpolate(copy.videos.count, { n: visible.length })}</p>
            <label className="search-field">
              <span className="visually-hidden">{copy.videos.searchLabel}</span>
              <input
                type="search"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder={copy.videos.searchPlaceholder}
              />
            </label>
            <div className="filter-row" role="group" aria-label={copy.videos.colStatus}>
              {STATUS_FILTERS.map((key) => (
                <button
                  key={key}
                  type="button"
                  className={statusFilter === key ? 'filter-chip is-on' : 'filter-chip'}
                  aria-pressed={statusFilter === key}
                  onClick={() => setStatusFilter(key)}
                >
                  {key === 'all' ? copy.videos.filterAll : lifecycleStatusLabel(key)}
                </button>
              ))}
            </div>
          </div>

          <div className="video-table" role="table" aria-label={copy.videos.title}>
            <div className="video-head" role="row">
              <span role="columnheader">{copy.videos.colFile}</span>
              <span role="columnheader">{copy.videos.colDate}</span>
              <span role="columnheader">{copy.videos.colStatus}</span>
              <span role="columnheader" className="video-head-action">{copy.videos.openDetail}</span>
            </div>
            {visible.length === 0 ? (
              <div className="empty-state compact">
                <p>{copy.videos.filterEmpty}</p>
                <p className="page-note">{copy.videos.filterEmptyHint}</p>
              </div>
            ) : (
              visible.map((video) => {
                const status = lifecycleStatus(video)
                return (
                  <div key={video.id} className="video-row" role="row">
                    <span className="file-name" role="cell">{video.originalFilename}</span>
                    <span className="file-date" role="cell">
                      {video.uploadedAt ? formatDateTime(video.uploadedAt) : '—'}
                    </span>
                    <span role="cell">
                      <span className={`status-badge is-${status}`}>{lifecycleStatusLabel(status)}</span>
                    </span>
                    <span role="cell" className="row-action">
                      <button type="button" className="btn-quiet" onClick={() => onOpen(video.id)}>
                        {copy.videos.openDetail}
                      </button>
                    </span>
                  </div>
                )
              })
            )}
          </div>
        </>
      )}

      {nextCursor && !failed && !isLoading && (
        <button type="button" className="btn-quiet load-more" onClick={() => { void loadMore() }} disabled={isLoadingMore}>
          {isLoadingMore ? copy.videos.loadMorePending : copy.videos.loadMore}
        </button>
      )}
    </section>
  )
}
