import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { AuthenticationService } from '../domain/authentication'
import { DEMO_USER } from '../infrastructure/mock-authentication-service'
import { useSessionBootstrap } from './use-session-bootstrap'

describe('useSessionBootstrap', () => {
  it('ignores a late bootstrap result after unmount', async () => {
    let resolveBootstrap!: (value: { user: typeof DEMO_USER }) => void
    const authenticationService = {
      bootstrap: vi.fn().mockImplementation(
        () => new Promise((resolve) => {
          resolveBootstrap = resolve
        }),
      ),
    } as unknown as AuthenticationService

    const { result, unmount } = renderHook(() => useSessionBootstrap(authenticationService))
    expect(result.current.checking).toBe(true)
    unmount()
    resolveBootstrap({ user: DEMO_USER })
    await Promise.resolve()
    expect(result.current.restoredUser).toBeNull()
  })

  it('restores an authenticated account', async () => {
    const authenticationService = {
      bootstrap: vi.fn().mockResolvedValue({ user: DEMO_USER }),
    } as unknown as AuthenticationService

    const { result } = renderHook(() => useSessionBootstrap(authenticationService))
    await waitFor(() => expect(result.current.checking).toBe(false))
    expect(result.current.restoredUser).toEqual(DEMO_USER)
  })
})
