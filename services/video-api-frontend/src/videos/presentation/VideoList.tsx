import { useEffect, useRef, useState } from 'react'
import { copy, interpolate } from '../../product-copy'
import { formatDateTime } from '../application/format-datetime'
import { lifecycleStatusLabel, lifecycleTone } from '../application/product-status'
import type {
  PrototypeScenario,
  VideoLibraryItem,
  VideoLibraryQuery,
  VideoLibrarySort,
  VideoLibrarySortDirection,
  VideoLibraryStatusFilter,
  VideoService,
} from '../domain/video'
import { DownloadResultButton } from './DownloadResultButton'

interface VideoListProps {
  videoService: VideoService
  scenario: PrototypeScenario
  onOpen: (videoRef: string) => void
  onUpload: () => void
}

const STATUS_FILTERS: Array<{ value: VideoLibraryStatusFilter; label: string }> = [
  { value: 'ALL', label: copy.videos.statusAll },
  { value: 'PROCESSED', label: copy.videos.statusProcessed },
  { value: 'PROCESSING', label: copy.videos.statusProcessing },
  { value: 'FAILED', label: copy.videos.statusFailed },
]

function SearchIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true" className="control-icon">
      <circle cx="8.5" cy="8.5" r="5.25" />
      <path d="m12.4 12.4 4.1 4.1" />
    </svg>
  )
}

function FilterIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true" className="control-icon">
      <path d="M3 4.5h14l-5.4 6.1v4.15l-3.2 1.6V10.6L3 4.5Z" />
    </svg>
  )
}

function ChevronIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true" className="chevron-icon">
      <path d="m4 6 4 4 4-4" />
    </svg>
  )
}

function SortIcon({ direction }: { direction: VideoLibrarySortDirection }) {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true" className={`sort-icon is-${direction.toLowerCase()}`}>
      <path className="sort-up" d="m5 7 3-3 3 3" />
      <path className="sort-down" d="m5 9 3 3 3-3" />
    </svg>
  )
}

export function VideoList({ videoService, scenario, onOpen, onUpload }: VideoListProps) {
  const [items, setItems] = useState<VideoLibraryItem[]>([])
  const [page, setPage] = useState(1)
  const [totalItems, setTotalItems] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [failed, setFailed] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [nameDraft, setNameDraft] = useState('')
  const [query, setQuery] = useState<VideoLibraryQuery>({ status: 'ALL' })
  const [isStatusMenuOpen, setIsStatusMenuOpen] = useState(false)
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const statusMenuRef = useRef<HTMLDivElement>(null)
  const statusTriggerRef = useRef<HTMLButtonElement>(null)

  useEffect(() => () => {
    if (debounceTimer.current) clearTimeout(debounceTimer.current)
  }, [])

  useEffect(() => {
    if (!isStatusMenuOpen) return
    function closeOnOutsidePress(event: PointerEvent) {
      if (!statusMenuRef.current?.contains(event.target as Node)) setIsStatusMenuOpen(false)
    }
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key !== 'Escape') return
      setIsStatusMenuOpen(false)
      statusTriggerRef.current?.focus()
    }
    document.addEventListener('pointerdown', closeOnOutsidePress)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsidePress)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [isStatusMenuOpen])

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
      .list(page, { scenario, query })
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
  }, [videoService, scenario, page, query, reloadKey])

  function cancelDebounce() {
    if (!debounceTimer.current) return
    clearTimeout(debounceTimer.current)
    debounceTimer.current = null
  }

  function applyName(name: string, match: 'PREFIX' | 'EXACT') {
    const normalized = name.trim()
    setPage(1)
    setQuery((current) => ({
      ...current,
      name: normalized || undefined,
      match: normalized ? match : undefined,
    }))
  }

  function handleNameChange(value: string) {
    setNameDraft(value)
    cancelDebounce()
    debounceTimer.current = setTimeout(() => {
      debounceTimer.current = null
      applyName(value, 'PREFIX')
    }, 300)
  }

  function handleExactSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    cancelDebounce()
    applyName(nameDraft, 'EXACT')
  }

  function handleStatusChange(status: VideoLibraryStatusFilter) {
    setIsStatusMenuOpen(false)
    statusTriggerRef.current?.focus()
    if (query.status === status) return
    setPage(1)
    setQuery((current) => ({ ...current, status }))
  }

  function handleSort(sort: VideoLibrarySort) {
    setPage(1)
    setQuery((current) => {
      const currentSort = current.sort ?? 'UPDATED_AT'
      const currentDirection = current.direction ?? 'DESC'
      const direction: VideoLibrarySortDirection = currentSort === sort
        ? currentDirection === 'ASC' ? 'DESC' : 'ASC'
        : sort === 'STATUS' ? 'ASC' : 'DESC'
      return { ...current, sort, direction }
    })
  }

  function clearCriteria() {
    cancelDebounce()
    setNameDraft('')
    setPage(1)
    setQuery((current) => ({ status: 'ALL', sort: current.sort, direction: current.direction }))
  }

  const appliedName = query.name?.trim()
  const hasAppliedCriteria = Boolean(appliedName) || query.status !== 'ALL'
  const hasVisibleCriteria = Boolean(nameDraft.trim()) || hasAppliedCriteria
  const selectedStatus = STATUS_FILTERS.find((filter) => filter.value === query.status) ?? STATUS_FILTERS[0]
  const activeSort = query.sort ?? 'UPDATED_AT'
  const activeDirection = query.direction ?? 'DESC'

  function sortLabel(column: string, sort: VideoLibrarySort) {
    const normalizedColumn = column.toLocaleLowerCase('pt-BR')
    if (activeSort !== sort) {
      return interpolate(copy.videos.sortByColumn, { column: normalizedColumn })
    }
    const direction = activeSort === sort ? activeDirection : sort === 'STATUS' ? 'ASC' : 'DESC'
    return interpolate(copy.videos.sortBy, {
      column: normalizedColumn,
      direction: direction === 'ASC' ? copy.videos.sortAscending : copy.videos.sortDescending,
    })
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

      <div className="list-toolbar">
        <form className="search-field" role="search" onSubmit={handleExactSearch}>
          <label htmlFor="video-name-search" className="visually-hidden">{copy.videos.searchLabel}</label>
          <span className="search-control">
            <SearchIcon />
            <input
              id="video-name-search"
              type="search"
              value={nameDraft}
              maxLength={512}
              placeholder={copy.videos.searchPlaceholder}
              onChange={(event) => handleNameChange(event.target.value)}
            />
          </span>
          <button type="submit" className="visually-hidden">{copy.videos.searchExactAction}</button>
        </form>

        <div className="status-filter" ref={statusMenuRef}>
          <button
            ref={statusTriggerRef}
            type="button"
            className={query.status === 'ALL' ? 'filter-trigger' : 'filter-trigger is-active'}
            aria-label={`${copy.videos.statusFilterLabel}: ${selectedStatus.label}`}
            aria-haspopup="menu"
            aria-expanded={isStatusMenuOpen}
            aria-controls="status-filter-menu"
            onClick={() => setIsStatusMenuOpen((open) => !open)}
          >
            <FilterIcon />
            <span>{query.status === 'ALL' ? copy.videos.statusFilterButton : selectedStatus.label}</span>
            <ChevronIcon />
          </button>
          {isStatusMenuOpen && (
            <div id="status-filter-menu" className="filter-menu" role="menu" aria-label={copy.videos.statusFilterLabel}>
            {STATUS_FILTERS.map((filter) => (
              <button
                key={filter.value}
                type="button"
                role="menuitemradio"
                className={query.status === filter.value ? 'filter-option is-selected' : 'filter-option'}
                aria-checked={query.status === filter.value}
                onClick={() => handleStatusChange(filter.value)}
              >
                <span>{filter.label}</span>
                <svg viewBox="0 0 16 16" className="filter-check" aria-hidden="true">
                  <path d="m3.5 8.2 2.8 2.8 6.2-6.2" />
                </svg>
              </button>
            ))}
            </div>
          )}
        </div>
        {hasVisibleCriteria && (
          <button type="button" className="btn-quiet clear-criteria" onClick={clearCriteria}>
            {copy.videos.clearCriteria}
          </button>
        )}
        <span className="visually-hidden" role="status" aria-live="polite" aria-atomic="true">
          {isLoading || scenario.kind === 'loading' ? copy.videos.loading : failed ? copy.videos.error : ''}
        </span>
      </div>

      {isLoading || scenario.kind === 'loading' ? (
        <p className="empty-state">{copy.videos.loading}</p>
      ) : failed ? (
        <div className="empty-state" role="alert">
          <p>{copy.videos.error}</p>
          <button type="button" className="btn-primary" onClick={() => setReloadKey((value) => value + 1)}>
            {copy.videos.retry}
          </button>
        </div>
      ) : totalItems === 0 && hasAppliedCriteria ? (
        <div className="empty-state">
          <h2>{copy.videos.filteredEmptyTitle}</h2>
          <p>{copy.videos.filteredEmptyBody}</p>
          <button type="button" className="btn-quiet empty-clear" onClick={clearCriteria}>
            {copy.videos.clearCriteria}
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
              <span role="columnheader" aria-sort={activeSort === 'UPDATED_AT' ? (activeDirection === 'ASC' ? 'ascending' : 'descending') : 'none'}>
                <button type="button" className={activeSort === 'UPDATED_AT' ? 'sort-button is-active' : 'sort-button'} aria-label={sortLabel(copy.videos.colDate, 'UPDATED_AT')} onClick={() => handleSort('UPDATED_AT')}>
                  {copy.videos.colDate}
                  <SortIcon direction={activeSort === 'UPDATED_AT' ? activeDirection : 'DESC'} />
                </button>
              </span>
              <span role="columnheader" aria-sort={activeSort === 'STATUS' ? (activeDirection === 'ASC' ? 'ascending' : 'descending') : 'none'}>
                <button type="button" className={activeSort === 'STATUS' ? 'sort-button is-active' : 'sort-button'} aria-label={sortLabel(copy.videos.colStatus, 'STATUS')} onClick={() => handleSort('STATUS')}>
                  {copy.videos.colStatus}
                  <SortIcon direction={activeSort === 'STATUS' ? activeDirection : 'ASC'} />
                </button>
              </span>
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
