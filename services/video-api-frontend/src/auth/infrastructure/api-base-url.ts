export const DEFAULT_VIDEO_API_BASE_URL = 'http://localhost:8080'

export function videoApiBaseUrl(value = import.meta.env.VITE_VIDEO_API_BASE_URL): string {
  const trimmed = value?.trim()
  if (!trimmed) {
    return DEFAULT_VIDEO_API_BASE_URL
  }
  return trimmed.replace(/\/+$/, '')
}
