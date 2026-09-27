import { act, fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { copy } from '../../product-copy'
import type { VideoLibraryPage, VideoService } from '../domain/video'
import { MockVideoService } from '../infrastructure/mock-video-service'
import { VideoList } from './VideoList'

const scenario = { kind: 'default' } as const

function page(filename?: string): VideoLibraryPage {
  return {
    items: filename ? [{
      videoRef: `ref-${filename}`,
      originalFilename: filename,
      status: 'AVAILABLE',
      jobId: null,
      submittedAt: '2026-09-20T10:00:00Z',
      activityAt: '2026-09-20T10:00:00Z',
    }] : [],
    page: 1,
    pageSize: 5,
    totalItems: filename ? 1 : 0,
    totalPages: filename ? 1 : 0,
  }
}

function serviceWith(list: VideoService['list']): VideoService {
  return {
    list,
    get: vi.fn(),
    download: vi.fn(),
    simulateUpload: vi.fn(),
  }
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((complete) => { resolve = complete })
  return { promise, resolve }
}

afterEach(() => {
  vi.useRealTimers()
})

describe('VideoList query controls', () => {
  it('applies prefix after 300 ms and Enter as an immediate exact search', async () => {
    vi.useFakeTimers()
    const list = vi.fn().mockResolvedValue(page())
    render(
      <VideoList videoService={serviceWith(list)} scenario={scenario} onOpen={vi.fn()} onUpload={vi.fn()} />,
    )
    await act(async () => undefined)
    const input = screen.getByRole('searchbox', { name: copy.videos.searchLabel })

    fireEvent.change(input, { target: { value: 'Bla' } })
    await act(async () => { vi.advanceTimersByTime(299) })
    expect(list).toHaveBeenCalledTimes(1)
    await act(async () => { vi.advanceTimersByTime(1) })
    expect(list).toHaveBeenLastCalledWith(1, {
      scenario,
      query: { name: 'Bla', match: 'PREFIX', status: 'ALL' },
    })

    fireEvent.change(input, { target: { value: 'Blackstock.mp4' } })
    fireEvent.submit(input.closest('form')!)
    await act(async () => undefined)
    expect(list).toHaveBeenLastCalledWith(1, {
      scenario,
      query: { name: 'Blackstock.mp4', match: 'EXACT', status: 'ALL' },
    })
    const callCount = list.mock.calls.length
    await act(async () => { vi.advanceTimersByTime(300) })
    expect(list).toHaveBeenCalledTimes(callCount)
  })

  it('ignores a response from an obsolete query', async () => {
    const oldRequest = deferred<VideoLibraryPage>()
    const currentRequest = deferred<VideoLibraryPage>()
    const list = vi.fn()
      .mockReturnValueOnce(oldRequest.promise)
      .mockReturnValueOnce(currentRequest.promise)
    const user = userEvent.setup()
    render(
      <VideoList videoService={serviceWith(list)} scenario={scenario} onOpen={vi.fn()} onUpload={vi.fn()} />,
    )

    await user.click(screen.getByRole('button', { name: `${copy.videos.statusFilterLabel}: ${copy.videos.statusAll}` }))
    await user.click(screen.getByRole('menuitemradio', { name: copy.videos.statusProcessed }))
    await act(async () => { currentRequest.resolve(page('atual.mp4')) })
    expect(await screen.findByText('atual.mp4')).toBeInTheDocument()
    await act(async () => { oldRequest.resolve(page('obsoleto.mp4')) })
    expect(screen.queryByText('obsoleto.mp4')).not.toBeInTheDocument()
    expect(screen.getByText('atual.mp4')).toBeInTheDocument()
  })

  it('resets pagination for status changes and clears every criterion', async () => {
    const videoService = new MockVideoService()
    const list = vi.spyOn(videoService, 'list')
    const user = userEvent.setup()
    render(
      <VideoList videoService={videoService} scenario={scenario} onOpen={vi.fn()} onUpload={vi.fn()} />,
    )
    await screen.findByText('campanha.mp4')

    await user.click(screen.getByRole('button', { name: copy.videos.pageLabel.replace('{n}', '2') }))
    expect(list).toHaveBeenLastCalledWith(2, { scenario, query: { status: 'ALL' } })
    await user.click(screen.getByRole('button', { name: `${copy.videos.statusFilterLabel}: ${copy.videos.statusAll}` }))
    await user.click(screen.getByRole('menuitemradio', { name: copy.videos.statusFailed }))
    expect(list).toHaveBeenLastCalledWith(1, { scenario, query: { status: 'FAILED' } })
    expect(screen.getByRole('button', { name: `${copy.videos.statusFilterLabel}: ${copy.videos.statusFailed}` })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: copy.videos.clearCriteria }))
    expect(list).toHaveBeenLastCalledWith(1, { scenario, query: { status: 'ALL' } })
    expect(screen.getByRole('button', { name: `${copy.videos.statusFilterLabel}: ${copy.videos.statusAll}` })).toBeInTheDocument()
  })

  it('sorts by status and updated date through the server query', async () => {
    const list = vi.fn().mockResolvedValue(page('aula.mp4'))
    const user = userEvent.setup()
    render(
      <VideoList videoService={serviceWith(list)} scenario={scenario} onOpen={vi.fn()} onUpload={vi.fn()} />,
    )
    await screen.findByText('aula.mp4')

    const statusHeader = screen.getByRole('columnheader', { name: copy.videos.colStatus })
    expect(statusHeader).toHaveAttribute('aria-sort', 'none')
    await user.click(screen.getByRole('button', { name: /Ordenar por status/ }))
    expect(list).toHaveBeenLastCalledWith(1, {
      scenario,
      query: { status: 'ALL', sort: 'STATUS', direction: 'ASC' },
    })
    expect(statusHeader).toHaveAttribute('aria-sort', 'ascending')

    await user.click(screen.getByRole('button', { name: /Ordenar por status/ }))
    expect(list).toHaveBeenLastCalledWith(1, {
      scenario,
      query: { status: 'ALL', sort: 'STATUS', direction: 'DESC' },
    })

    await user.click(screen.getByRole('button', { name: /Ordenar por atualizado em/ }))
    expect(list).toHaveBeenLastCalledWith(1, {
      scenario,
      query: { status: 'ALL', sort: 'UPDATED_AT', direction: 'DESC' },
    })
  })

  it('aligns action content consistently and marks only results still being prepared', async () => {
    const items: VideoLibraryPage['items'] = [
      {
        videoRef: 'ref-processing', originalFilename: 'processando.mp4', status: 'PROCESSING', jobId: 'job-processing',
        submittedAt: '2026-09-20T10:00:00Z', activityAt: '2026-09-20T10:00:00Z',
      },
      {
        videoRef: 'ref-failed', originalFilename: 'falha.mp4', status: 'FAILED', jobId: 'job-failed',
        submittedAt: '2026-09-20T09:00:00Z', activityAt: '2026-09-20T09:00:00Z',
      },
      {
        videoRef: 'ref-available', originalFilename: 'pronto.mp4', status: 'AVAILABLE', jobId: 'job-available',
        submittedAt: '2026-09-20T08:00:00Z', activityAt: '2026-09-20T08:00:00Z',
      },
    ]
    const list = vi.fn().mockResolvedValue({ items, page: 1, pageSize: 5, totalItems: 3, totalPages: 1 })
    render(
      <VideoList videoService={serviceWith(list)} scenario={scenario} onOpen={vi.fn()} onUpload={vi.fn()} />,
    )

    const processingRow = await screen.findByRole('row', { name: /processando\.mp4/ })
    expect(within(processingRow).getByLabelText(copy.videos.downloadPreparing)).toBeInTheDocument()
    const failedRow = screen.getByRole('row', { name: /falha\.mp4/ })
    expect(within(failedRow).queryByLabelText(copy.videos.downloadPreparing)).not.toBeInTheDocument()
    const availableRow = screen.getByRole('row', { name: /pronto\.mp4/ })
    expect(within(availableRow).getByRole('button', { name: copy.videos.download })).toBeInTheDocument()
    expect(within(availableRow).queryByLabelText(copy.videos.downloadPreparing)).not.toBeInTheDocument()
  })

  it('distinguishes a filtered empty state and retries with the active criteria', async () => {
    vi.useFakeTimers()
    const list = vi.fn()
      .mockResolvedValueOnce(page())
      .mockRejectedValueOnce({ code: 'LIST_UNAVAILABLE' })
      .mockResolvedValueOnce(page())
    render(
      <VideoList videoService={serviceWith(list)} scenario={scenario} onOpen={vi.fn()} onUpload={vi.fn()} />,
    )
    await act(async () => undefined)

    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'inexistente' } })
    await act(async () => { vi.advanceTimersByTime(300) })
    expect(screen.getByRole('alert')).toHaveTextContent(copy.videos.error)
    fireEvent.click(screen.getByRole('button', { name: copy.videos.retry }))
    await act(async () => undefined)

    expect(list).toHaveBeenLastCalledWith(1, {
      scenario,
      query: { name: 'inexistente', match: 'PREFIX', status: 'ALL' },
    })
    expect(screen.getByRole('heading', { name: copy.videos.filteredEmptyTitle })).toBeInTheDocument()
    expect(screen.queryByText(copy.videos.emptyTitle)).not.toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: copy.videos.clearCriteria }).length).toBeGreaterThan(0)
  })
})
