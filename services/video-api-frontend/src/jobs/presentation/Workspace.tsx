import { useCallback, useEffect, useState } from 'react'
import type { AuthenticatedUser } from '../../auth/domain/authentication'
import { JOB_STATUS_COPY, type Job, type JobService } from '../domain/job'
import { CreateJobForm } from './CreateJobForm'
import { JobDetail } from './JobDetail'

interface WorkspaceProps {
  user: AuthenticatedUser
  jobService: JobService
  onLogout: () => void
}

const PAGE_SIZE = 20
const LOAD_ERROR = 'Não foi possível carregar os jobs. Tente novamente.'

function formatUtc(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return iso
  }
  return date.toISOString().replace('T', ' ').replace(/\.\d{3}Z$/, ' UTC')
}

export function Workspace({ user, jobService, onLogout }: WorkspaceProps) {
  const [jobs, setJobs] = useState<Job[]>([])
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isLoadingMore, setIsLoadingMore] = useState(false)

  const selectedJob = jobs.find((job) => job.id === selectedId) ?? null

  const loadPage = useCallback(async (cursor?: string) => {
    const page = await jobService.list(user.id, { cursor, limit: PAGE_SIZE })
    return page
  }, [jobService, user.id])

  useEffect(() => {
    let cancelled = false
    setIsLoading(true)
    setLoadError(null)
    loadPage()
      .then((page) => {
        if (cancelled) return
        setJobs(page.items)
        setNextCursor(page.nextCursor)
      })
      .catch(() => {
        if (!cancelled) setLoadError(LOAD_ERROR)
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [loadPage])

  async function handleLogout() {
    await Promise.resolve()
    onLogout()
  }

  async function handleLoadMore() {
    if (!nextCursor || isLoadingMore) return
    setIsLoadingMore(true)
    setLoadError(null)
    try {
      const page = await loadPage(nextCursor)
      setJobs((current) => [...current, ...page.items])
      setNextCursor(page.nextCursor)
    } catch {
      setLoadError(LOAD_ERROR)
    } finally {
      setIsLoadingMore(false)
    }
  }

  function handleCreated(job: Job) {
    setJobs((current) => [job, ...current.filter((item) => item.id !== job.id)])
    setSelectedId(job.id)
  }

  function selectJob(id: string) {
    setSelectedId(id)
    if (window.matchMedia?.('(max-width: 980px)')?.matches) {
      const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches
      document.getElementById('job-panel')?.scrollIntoView({
        behavior: reduceMotion ? 'auto' : 'smooth',
        block: 'start',
      })
    }
  }

  return (
    <div className="workspace">
      <header className="workspace-bar">
        <div className="wordmark" aria-label="FIAP X">FIAP <span>X</span></div>
        <p className="workspace-index">JOBS DO PROPRIETÁRIO · GET /v1/jobs</p>
        <div className="session">
          <p>
            <strong>{user.email}</strong>
            <span>{user.roles.join(', ')}</span>
          </p>
          <button type="button" className="ghost compact" onClick={() => { void handleLogout() }}>
            Sair
          </button>
        </div>
      </header>

      <div className="workspace-body">
        <section className="job-board" aria-labelledby="jobs-title">
          <div className="board-heading">
            <p className="sheet-index">LISTAGEM POR CURSOR</p>
            <h1 id="jobs-title">Meus jobs</h1>
            <p className="product-summary">
              Somente jobs desta conta. Recurso de terceiro responde 404, igual a recurso inexistente.
            </p>
          </div>

          {loadError && <p className="alert" role="alert">{loadError}</p>}
          {isLoading ? (
            <p className="empty-state">Carregando jobs…</p>
          ) : jobs.length === 0 ? (
            <p className="empty-state">Nenhum job nesta conta. Crie o primeiro a partir de uma sourceKey confirmada.</p>
          ) : (
            <ul className="job-list">
              {jobs.map((job) => (
                <li key={job.id}>
                  <button
                    type="button"
                    className={job.id === selectedId ? 'job-row is-selected' : 'job-row'}
                    onClick={() => selectJob(job.id)}
                    aria-current={job.id === selectedId ? 'true' : undefined}
                  >
                    <span className={`status-chip status-${job.status.toLowerCase()}`}>{job.status}</span>
                    <strong>{JOB_STATUS_COPY[job.status].label}</strong>
                    <code>{job.sourceKey}</code>
                    <time dateTime={job.createdAt}>{formatUtc(job.createdAt)}</time>
                  </button>
                </li>
              ))}
            </ul>
          )}

          {nextCursor && (
            <button type="button" className="ghost" onClick={() => { void handleLoadMore() }} disabled={isLoadingMore}>
              {isLoadingMore ? 'Carregando…' : 'Carregar mais'}
            </button>
          )}

          <ol className="status-legend" aria-label="Estados do job">
            {Object.entries(JOB_STATUS_COPY).map(([status, copy]) => (
              <li key={status}>
                <b>{status}</b> {copy.label}
              </li>
            ))}
          </ol>
        </section>

        <aside className="job-panel" id="job-panel">
          {selectedJob ? (
            <JobDetail job={selectedJob} onCreateAnother={() => setSelectedId(null)} />
          ) : (
            <CreateJobForm userId={user.id} jobService={jobService} onCreated={handleCreated} />
          )}
        </aside>
      </div>
    </div>
  )
}
