import { type ChangeEvent, type FormEvent, useRef, useState } from 'react'
import { copy, interpolate } from '../../product-copy'
import { formatFileSize } from '../application/format-datetime'
import { playUploadProgress, uploadStepMs, wait } from '../application/upload-flow'
import { validateUploadSelection } from '../application/validate-upload'
import type { PrototypeScenario, VideoService } from '../domain/video'

interface UploadVideoProps {
  userId: string
  videoService: VideoService
  scenario: PrototypeScenario
  onFinished: () => void
}

type UploadPhase = 'form' | 'sending' | 'confirming' | 'success' | 'error' | 'pending'

export function UploadVideo({ userId, videoService, scenario, onFinished }: UploadVideoProps) {
  const [file, setFile] = useState<File | null>(null)
  const [fieldError, setFieldError] = useState<string | null>(null)
  const [progress, setProgress] = useState(0)
  const [phase, setPhase] = useState<UploadPhase>('form')
  const [isBusy, setIsBusy] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  function applyFile(next: File | null) {
    setPhase('form')
    setProgress(0)
    if (!next) {
      setFile(null)
      setFieldError(null)
      return
    }
    const validation = validateUploadSelection({
      name: next.name,
      sizeBytes: next.size,
      contentType: next.type,
    })
    setFieldError(validation.errors.file ?? null)
    setFile(validation.selection ? next : null)
  }

  function handleFile(event: ChangeEvent<HTMLInputElement>) {
    applyFile(event.target.files?.[0] ?? null)
  }

  function clearFile() {
    applyFile(null)
    if (inputRef.current) {
      inputRef.current.value = ''
    }
  }

  async function confirmUpload() {
    if (!file) return
    const validation = validateUploadSelection({
      name: file.name,
      sizeBytes: file.size,
      contentType: file.type,
    })
    if (!validation.selection) {
      setFieldError(validation.errors.file ?? copy.upload.missingFile)
      setPhase('form')
      return
    }

    setIsBusy(true)
    setPhase('confirming')
    setProgress(100)
    try {
      await wait(uploadStepMs() === 0 ? 0 : 420)
      if (scenario.kind === 'upload-error') {
        setPhase('error')
        return
      }
      await videoService.simulateUpload(userId, validation.selection, { scenario })
      setPhase('success')
    } catch {
      setPhase('error')
    } finally {
      setIsBusy(false)
    }
  }

  async function startUpload() {
    if (!file || isBusy) return
    setIsBusy(true)
    setPhase('sending')
    setProgress(0)
    try {
      await playUploadProgress(setProgress, uploadStepMs())
      if (scenario.kind === 'upload-pending') {
        setPhase('pending')
        return
      }
      await confirmUpload()
    } finally {
      setIsBusy(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) return
    if (phase === 'pending' || phase === 'error') {
      await confirmUpload()
      return
    }
    await startUpload()
  }

  const canSubmit = Boolean(file) && !fieldError && !isBusy && phase === 'form'

  return (
    <section className="page-block" aria-labelledby="upload-title">
      <h1 id="upload-title">{copy.upload.title}</h1>
      <p className="page-lead">{copy.upload.lead}</p>

      {phase === 'success' ? (
        <div className="result-card" role="status">
          <span className="result-icon is-success" aria-hidden="true" />
          <h2>{copy.upload.successTitle}</h2>
          <p>{copy.upload.successCopy}</p>
          <p className="status-badge is-uploaded">{copy.upload.successStatus}</p>
          <div className="result-actions">
            <button type="button" onClick={onFinished}>{copy.upload.successAction}</button>
            <button type="button" className="ghost" onClick={clearFile}>{copy.upload.sendAnother}</button>
          </div>
        </div>
      ) : (
        <form className="upload-form" noValidate onSubmit={(event) => { void handleSubmit(event) }}>
          {(phase === 'form' || phase === 'error' || phase === 'pending') && (
            <div className="field">
              <label htmlFor="video-file">{copy.upload.fileLabel}</label>
              <input
                ref={inputRef}
                id="video-file"
                name="file"
                type="file"
                className="file-input"
                accept=".mp4,.mov,.webm,.mkv,video/mp4,video/quicktime,video/webm,video/x-matroska"
                onChange={handleFile}
                aria-invalid={Boolean(fieldError)}
                aria-describedby={fieldError ? 'file-error' : undefined}
              />
              {!file && (
                <button
                  type="button"
                  className="drop-zone"
                  onClick={() => inputRef.current?.click()}
                >
                  <strong>{copy.upload.dropIdle}</strong>
                  <span>{copy.upload.dropHint}</span>
                </button>
              )}
              {file && (
                <div className="selected-file">
                  <p>{interpolate(copy.upload.selected, { nome: file.name, tamanho: formatFileSize(file.size) })}</p>
                  <button type="button" className="text-link" onClick={clearFile}>
                    {copy.upload.removeFile}
                  </button>
                </div>
              )}
              {fieldError && <p id="file-error" className="field-error" role="alert">{fieldError}</p>}
            </div>
          )}

          {(phase === 'sending' || phase === 'confirming') && (
            <div className="progress-card" role="status">
              <h2>{phase === 'sending' ? copy.upload.sendingTitle : copy.upload.confirmingTitle}</h2>
              <p>{phase === 'sending' ? copy.upload.sendingCopy : copy.upload.confirmingCopy}</p>
              <div className="progress-track" aria-hidden="true">
                <div className={phase === 'confirming' ? 'progress-fill is-complete' : 'progress-fill'} style={{ width: `${progress}%` }} />
              </div>
              {phase === 'sending' ? (
                <p>{interpolate(copy.upload.percent, { percent: progress })}</p>
              ) : (
                <span className="confirm-mark" aria-hidden="true" />
              )}
            </div>
          )}

          {phase === 'error' && (
            <div className="result-card is-error" role="alert">
              <span className="result-icon is-error" aria-hidden="true" />
              <h2>{copy.upload.errorTitle}</h2>
              <p>{copy.upload.errorCopy}</p>
            </div>
          )}

          {phase === 'pending' && (
            <div className="result-card" role="status">
              <p className="status-badge is-pending">{copy.upload.pendingStatus}</p>
              <h2>{copy.upload.pendingCopy}</h2>
              <p className="page-note">{copy.upload.pendingHint}</p>
            </div>
          )}

          {phase === 'form' && (
            <button type="submit" disabled={!canSubmit}>
              {copy.upload.submit}
            </button>
          )}
          {phase === 'sending' && (
            <button type="button" disabled>{copy.upload.sendingTitle}</button>
          )}
          {phase === 'error' && (
            <div className="result-actions">
              <button type="submit">{copy.upload.retry}</button>
              <button type="button" className="ghost" onClick={clearFile}>{copy.upload.chooseAnother}</button>
            </div>
          )}
          {phase === 'pending' && (
            <button type="submit">{copy.upload.confirmPending}</button>
          )}
        </form>
      )}
    </section>
  )
}
