import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { copy } from '../../product-copy'
import type { VideoService } from '../domain/video'
import { UploadVideo } from './UploadVideo'

function serviceWithUpload(upload: NonNullable<VideoService['upload']>): VideoService {
  return {
    list: vi.fn(),
    get: vi.fn(),
    download: vi.fn(),
    simulateUpload: vi.fn(),
    upload,
  }
}

async function chooseAndReview(user: ReturnType<typeof userEvent.setup>) {
  const file = new File(['video'], 'aula.webm', { type: 'video/webm' })
  await user.upload(screen.getByLabelText(copy.upload.fileLabel), file)
  await user.click(screen.getByRole('button', { name: copy.upload.submit }))
  expect(screen.getByRole('heading', { name: copy.upload.reviewTitle })).toBeInTheDocument()
}

describe('UploadVideo', () => {
  it('does not reserve or transfer bytes when the user cancels the review', async () => {
    const user = userEvent.setup()
    const upload = vi.fn<NonNullable<VideoService['upload']>>()
    render(
      <UploadVideo
        userId="user-1"
        videoService={serviceWithUpload(upload)}
        scenario={{ kind: 'default' }}
        onFinished={vi.fn()}
      />,
    )

    await chooseAndReview(user)
    await user.click(screen.getByRole('button', { name: copy.upload.cancel }))

    expect(upload).not.toHaveBeenCalled()
    expect(screen.queryByText(/aula\.webm/)).not.toBeInTheDocument()
  })

  it('keeps a terminal job failure distinct from a request error or success', async () => {
    const user = userEvent.setup()
    const upload = vi.fn<NonNullable<VideoService['upload']>>().mockImplementation(async (_file, options) => {
      options.onPhase('processing')
      options.onJobStatus('FAILED')
      return { videoId: 'video-1', jobId: 'job-1', status: 'FAILED' }
    })
    render(
      <UploadVideo
        userId="user-1"
        videoService={serviceWithUpload(upload)}
        scenario={{ kind: 'default' }}
        onFinished={vi.fn()}
      />,
    )

    await chooseAndReview(user)
    await user.click(screen.getByRole('button', { name: copy.upload.confirm }))

    expect(await screen.findByRole('heading', { name: copy.upload.processingFailed })).toBeInTheDocument()
    expect(screen.getByText(copy.lifecycleStatus.failed)).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: copy.upload.successTitle })).not.toBeInTheDocument()
  })
})
