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
    const open = vi.spyOn(window, 'open').mockReturnValue({} as Window)
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
    expect(open).toHaveBeenCalledWith('https://shorturl.at/JpxZS', '_blank', 'noopener,noreferrer')
    open.mockRestore()
  })

  it('shows a safe retryable message for unavailable storage', async () => {
    const user = userEvent.setup()
    const download = vi.fn().mockRejectedValue({ code: 'DOWNLOAD_STORAGE_UNAVAILABLE', message: 'internal' })
    render(<DownloadResultButton jobId="job-1" videoService={service(download)} />)

    await user.click(screen.getByRole('button', { name: copy.videos.download }))
    expect(await screen.findByRole('alert')).toHaveTextContent(copy.videos.downloadStorageUnavailable)
  })
})
