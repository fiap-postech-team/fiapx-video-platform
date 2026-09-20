import { copy } from '../../product-copy'
import type { UploadSelection } from '../domain/video'

export const ALLOWED_EXTENSIONS = ['.mp4', '.mov', '.webm', '.mkv'] as const
export const MAX_UPLOAD_BYTES = 500_000_000

export type UploadField = 'file'
export type UploadValidationErrors = Partial<Record<UploadField, string>>

export interface UploadValidationResult {
  selection?: UploadSelection
  errors: UploadValidationErrors
}

function extensionOf(name: string): string {
  const index = name.lastIndexOf('.')
  return index >= 0 ? name.slice(index).toLowerCase() : ''
}

export function validateUploadSelection(input: {
  name: string
  sizeBytes: number
  contentType: string
}): UploadValidationResult {
  const name = input.name.trim()
  if (!name) {
    return { errors: { file: copy.upload.missingFile } }
  }

  const extension = extensionOf(name)
  if (!ALLOWED_EXTENSIONS.includes(extension as (typeof ALLOWED_EXTENSIONS)[number])) {
    return { errors: { file: copy.upload.invalidType } }
  }

  if (input.sizeBytes > MAX_UPLOAD_BYTES) {
    return { errors: { file: copy.upload.invalidSize } }
  }

  return {
    selection: {
      name,
      sizeBytes: input.sizeBytes,
      contentType: input.contentType,
    },
    errors: {},
  }
}
