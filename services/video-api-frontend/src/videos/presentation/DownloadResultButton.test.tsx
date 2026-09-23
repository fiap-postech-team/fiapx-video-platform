import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { copy } from '../../product-copy'
import type { VideoService } from '../domain/video'
import { DownloadResultButton } from './DownloadResultButton'

function service(download: VideoService['download']): VideoService {
  return {
    list: vi.fn(),
    get: vi.fn(),
    download,
    simulateUpload: vi.fn(),
  }
}

describe('DownloadResultButton', () => {
  it('opens the temporary URL in a new tab and blocks duplicate clicks', async () => {
    const user = userEvent.setup()
    let resolve: ((value: Awaited<ReturnType<VideoService['download']>>) => void) | undefined
    const download = vi.fn(() => new Promise<Awaited<ReturnType<VideoService['download']>>>((next) => { resolve = next }))
    const replace = vi.fn()
    const popup = { opener: window, location: { replace }, close: vi.fn() } as unknown as Window
    const open = vi.spyOn(window, 'open').mockReturnValue(popup)
    render(<DownloadResultButton jobId="job-1" videoService={service(download)} />)

    const button = screen.getByRole('button', { name: copy.videos.download })
    await user.click(button)
    expect(button).toBeDisabled()
    await user.click(button)
    expect(download).toHaveBeenCalledTimes(1)

    resolve?.({
      downloadUrl: 'https://shorturl.at/JpxZS',
      expiresAt: '2099-01-01T00:00:00Z',
      filename: 'resultado-job-1.zip',
      contentType: 'application/zip',
      sizeBytes: 10,
    })
    expect(await screen.findByRole('button', { name: copy.videos.download })).toBeEnabled()
    expect(open).toHaveBeenCalledTimes(1)
    expect(open).toHaveBeenCalledWith('', '_blank')
    expect(popup.opener).toBeNull()
    expect(replace).toHaveBeenCalledWith('https://shorturl.at/JpxZS')
    const notification = await screen.findByRole('status')
    expect(notification).toHaveTextContent(copy.videos.downloadSuccess)
    expect(notification).toHaveClass('download-notification', 'download-notification-success')
    open.mockRestore()
  })

  it('uses a single fallback link when the browser blocks the pending tab', async () => {
    const user = userEvent.setup()
    const download = vi.fn().mockResolvedValue({
      downloadUrl: 'https://shorturl.at/JpxZS',
      expiresAt: '2099-01-01T00:00:00Z',
      filename: 'resultado-job-1.zip',
      contentType: 'application/zip',
      sizeBytes: 10,
    })
    const open = vi.spyOn(window, 'open').mockReturnValue(null)
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
    render(<DownloadResultButton jobId="job-1" videoService={service(download)} />)

    await user.click(screen.getByRole('button', { name: copy.videos.download }))

    expect(open).toHaveBeenCalledTimes(1)
    expect(click).toHaveBeenCalledTimes(1)
    open.mockRestore()
    click.mockRestore()
  })

  it('shows a safe retryable message for unavailable storage', async () => {
    const user = userEvent.setup()
    const download = vi.fn().mockRejectedValue({ code: 'DOWNLOAD_STORAGE_UNAVAILABLE', message: 'internal' })
    const close = vi.fn()
    const popup = { opener: window, location: { replace: vi.fn() }, close } as unknown as Window
    const open = vi.spyOn(window, 'open').mockReturnValue(popup)
    render(<DownloadResultButton jobId="job-1" videoService={service(download)} />)

    await user.click(screen.getByRole('button', { name: copy.videos.download }))
    expect(await screen.findByRole('alert')).toHaveTextContent(copy.videos.downloadStorageUnavailable)
    expect(close).toHaveBeenCalledTimes(1)
    open.mockRestore()
  })
})
