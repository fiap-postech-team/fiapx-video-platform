export class AccessTokenStore {
  private accessToken: string | null = null
  private expiresInSeconds = 0
  private epoch = 0
  private refreshInFlight: Promise<string> | null = null

  set(accessToken: string, expiresInSeconds: number): void {
    this.accessToken = accessToken
    this.expiresInSeconds = expiresInSeconds
  }

  get(): string | null {
    return this.accessToken
  }

  ttlSeconds(): number {
    return this.expiresInSeconds
  }

  clear(): void {
    this.epoch += 1
    this.accessToken = null
    this.expiresInSeconds = 0
  }

  currentEpoch(): number {
    return this.epoch
  }

  setIfEpoch(epoch: number, accessToken: string, expiresInSeconds: number): boolean {
    if (epoch !== this.epoch) {
      return false
    }
    this.set(accessToken, expiresInSeconds)
    return true
  }

  refreshOnce(refresh: () => Promise<string>): Promise<string> {
    const epoch = this.epoch
    if (!this.refreshInFlight) {
      this.refreshInFlight = refresh().finally(() => {
        this.refreshInFlight = null
      })
    }
    return this.refreshInFlight.then((token) => {
      if (epoch !== this.epoch) {
        throw new Error('session-cleared')
      }
      return token
    })
  }
}
