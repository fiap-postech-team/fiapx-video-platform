export const UPLOAD_PROGRESS_STEPS = [8, 22, 38, 54, 70, 86, 100]

export function wait(ms: number): Promise<void> {
  if (ms <= 0) {
    return Promise.resolve()
  }
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

export async function playUploadProgress(
  onProgress: (percent: number) => void,
  stepMs: number,
): Promise<void> {
  for (const percent of UPLOAD_PROGRESS_STEPS) {
    onProgress(percent)
    await wait(stepMs)
  }
}

export function uploadStepMs(): number {
  if (typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches) {
    return 0
  }
  return 70
}
