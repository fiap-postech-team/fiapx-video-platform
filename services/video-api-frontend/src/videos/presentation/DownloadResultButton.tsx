import { useState } from 'react'
import { copy } from '../../product-copy'
import { isVideoServiceError, type VideoService } from '../domain/video'

interface DownloadResultButtonProps {
  jobId: string
  videoService: VideoService
  className?: string
  iconOnly?: boolean
}

function DownloadIcon() {
  return (
    <svg className="download-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M12 3v11m0 0 4-4m-4 4-4-4M5 19h14" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
    </svg>
  )
}

export function DownloadResultButton({
  jobId,
  videoService,
  className = 'btn-quiet',
  iconOnly = false,
}: DownloadResultButtonProps) {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  async function handleDownload() {
    if (pending) return
    const popup = openPendingTab()
    setPending(true)
    setError(null)
    setSuccess(false)
    try {
      const result = await videoService.download(jobId)
      navigateToDownload(popup, result.downloadUrl, result.filename)
      setSuccess(true)
    } catch (cause) {
      popup?.close()
      setError(downloadMessage(cause))
    } finally {
      setPending(false)
    }
  }

  return (
    <span className="download-action">
      <button
        type="button"
        className={className}
        onClick={() => { void handleDownload() }}
        disabled={pending}
        aria-busy={pending}
        aria-label={pending ? copy.videos.downloadPending : copy.videos.download}
        title={iconOnly ? (pending ? copy.videos.downloadPending : copy.videos.download) : undefined}
      >
        <DownloadIcon />
        {!iconOnly && <span>{pending ? copy.videos.downloadPending : copy.videos.download}</span>}
      </button>
      {error && (
        <span className="download-notification" role="alert">
          <span className="download-notification-mark" aria-hidden="true" />
          {error}
        </span>
      )}
      {success && (
        <span className="download-success" role="status">
          {copy.videos.downloadSuccess}
        </span>
      )}
    </span>
  )
}

function openPendingTab(): Window | null {
  try {
    const popup = window.open('', '_blank')
    if (popup) popup.opener = null
    return popup
  } catch {
    return null
  }
}

function navigateToDownload(popup: Window | null, url: string, filename: string): void {
  if (popup) {
    try {
      popup.location.replace(url)
      return
    } catch {
      popup.close()
    }
  }
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.target = '_blank'
  anchor.rel = 'noopener noreferrer'
  anchor.download = filename
  anchor.click()
}

function downloadMessage(cause: unknown): string {
  if (!isVideoServiceError(cause)) return copy.videos.downloadInconsistent
  switch (cause.code) {
    case 'DOWNLOAD_NOT_READY': return copy.videos.downloadNotReady
    case 'DOWNLOAD_OBJECT_MISSING': return copy.videos.downloadMissing
    case 'DOWNLOAD_STORAGE_UNAVAILABLE': return copy.videos.downloadStorageUnavailable
    case 'DOWNLOAD_NOT_FOUND': return copy.videos.downloadNotFound
    default: return copy.videos.downloadInconsistent
  }
}
