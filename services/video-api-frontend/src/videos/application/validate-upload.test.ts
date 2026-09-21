import { describe, expect, it } from 'vitest'
import { copy } from '../../product-copy'
import { MAX_UPLOAD_BYTES, validateUploadSelection } from './validate-upload'

describe('validateUploadSelection', () => {
  it('accepts the four allowed formats up to 500 MB', () => {
    const formats = [
      ['aula.mp4', 'video/mp4'],
      ['aula.mov', 'video/quicktime'],
      ['aula.webm', 'video/webm'],
      ['aula.mkv', 'video/x-matroska'],
    ] as const
    for (const [name, contentType] of formats) {
      expect(validateUploadSelection({ name, sizeBytes: MAX_UPLOAD_BYTES, contentType: '' })).toEqual({
        selection: { name, sizeBytes: MAX_UPLOAD_BYTES, contentType },
        errors: {},
      })
    }
  })

  it('rejects a missing file, invalid extension and oversized file', () => {
    expect(validateUploadSelection({ name: '', sizeBytes: 10, contentType: 'video/mp4' })).toEqual({
      errors: { file: copy.upload.missingFile },
    })
    expect(validateUploadSelection({ name: 'notas.pdf', sizeBytes: 10, contentType: 'application/pdf' })).toEqual({
      errors: { file: copy.upload.invalidType },
    })
    expect(validateUploadSelection({ name: 'aula.mp4', sizeBytes: MAX_UPLOAD_BYTES + 1, contentType: 'video/mp4' })).toEqual({
      errors: { file: copy.upload.invalidSize },
    })
    expect(validateUploadSelection({ name: 'aula.mp4', sizeBytes: 0, contentType: 'video/mp4' })).toEqual({
      errors: { file: copy.upload.emptyFile },
    })
  })
})
