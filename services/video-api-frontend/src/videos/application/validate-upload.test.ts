import { describe, expect, it } from 'vitest'
import { copy } from '../../product-copy'
import { MAX_UPLOAD_BYTES, validateUploadSelection } from './validate-upload'

describe('validateUploadSelection', () => {
  it('accepts the four allowed formats up to 500 MB', () => {
    for (const name of ['aula.mp4', 'aula.mov', 'aula.webm', 'aula.mkv']) {
      expect(validateUploadSelection({ name, sizeBytes: MAX_UPLOAD_BYTES, contentType: '' })).toEqual({
        selection: { name, sizeBytes: MAX_UPLOAD_BYTES, contentType: '' },
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
  })
})
