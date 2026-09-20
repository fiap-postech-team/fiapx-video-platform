import { JOB_STATUS_COPY, type Job } from '../domain/job'

interface JobDetailProps {
  job: Job
  onCreateAnother: () => void
}

function formatUtc(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return iso
  }
  return `${date.toISOString().replace('T', ' ').replace(/\.\d{3}Z$/, ' UTC')}`
}

export function JobDetail({ job, onCreateAnother }: JobDetailProps) {
  const copy = JOB_STATUS_COPY[job.status]

  return (
    <article className="inspector" aria-labelledby="job-detail-title">
      <p className="panel-kicker">GET /v1/jobs/{job.id}</p>
      <h2 id="job-detail-title">Job do proprietário</h2>
      <p className={`status-chip status-${job.status.toLowerCase()}`}>
        {job.status} · {copy.label}
      </p>
      <p className="panel-copy">{copy.detail}</p>
      <dl className="inspector-grid">
        <div>
          <dt>id</dt>
          <dd><code>{job.id}</code></dd>
        </div>
        <div>
          <dt>userId</dt>
          <dd><code>{job.userId}</code></dd>
        </div>
        <div>
          <dt>sourceKey</dt>
          <dd><code>{job.sourceKey}</code></dd>
        </div>
        <div>
          <dt>resultKey</dt>
          <dd>{job.resultKey ? <code>{job.resultKey}</code> : '—'}</dd>
        </div>
        <div>
          <dt>createdAt</dt>
          <dd>{formatUtc(job.createdAt)}</dd>
        </div>
      </dl>
      {job.status === 'COMPLETED' && (
        <p className="panel-copy" role="note">
          Download permanece no roadmap. O ZIP já está identificado por <code>resultKey</code> no object storage.
        </p>
      )}
      {job.status === 'FAILED' && (
        <p className="panel-copy" role="note">
          A notificação de falha terminal é responsabilidade do notification-worker, não desta tela.
        </p>
      )}
      <button type="button" className="ghost" onClick={onCreateAnother}>
        Novo job
      </button>
    </article>
  )
}
