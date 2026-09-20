import { describe, expect, it, vi } from 'vitest'
import { AccessTokenStore } from './access-token-store'

describe('AccessTokenStore', () => {
  it('keeps the access token only in memory and drops it on clear', () => {
    const store = new AccessTokenStore()
    store.set('token-one', 900)

    expect(store.get()).toBe('token-one')
    expect(store.ttlSeconds()).toBe(900)
    store.clear()
    expect(store.get()).toBeNull()
    expect(store.ttlSeconds()).toBe(0)
  })

  it('shares a single in-flight refresh and ignores a result after clear', async () => {
    const store = new AccessTokenStore()
    let finish!: (value: string) => void
    const refresh = vi.fn().mockImplementation(
      () => new Promise<string>((resolve) => {
        finish = resolve
      }),
    )

    const first = store.refreshOnce(refresh)
    const second = store.refreshOnce(refresh)
    expect(refresh).toHaveBeenCalledTimes(1)

    store.clear()
    finish('new-token')

    await expect(first).rejects.toThrow('session-cleared')
    await expect(second).rejects.toThrow('session-cleared')
    expect(store.get()).toBeNull()
  })

  it('refuses to restore a token from a stale epoch', () => {
    const store = new AccessTokenStore()
    const epoch = store.currentEpoch()
    store.clear()

    expect(store.setIfEpoch(epoch, 'stale', 900)).toBe(false)
    expect(store.get()).toBeNull()
  })
})
