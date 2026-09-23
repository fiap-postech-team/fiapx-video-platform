import { useCallback, useEffect, useState } from 'react'
import type { AuthenticatedUser, AuthenticationService } from '../domain/authentication'

interface SessionBootstrapState {
  checking: boolean
  user: AuthenticatedUser | null
  errorMessage: string | null
}

export function useSessionBootstrap(authenticationService: AuthenticationService): {
  checking: boolean
  restoredUser: AuthenticatedUser | null
  bootstrapError: string | null
  retryBootstrap: () => void
} {
  const [state, setState] = useState<SessionBootstrapState>({
    checking: true,
    user: null,
    errorMessage: null,
  })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    setState((current) => ({ ...current, checking: true }))

    void (async () => {
      try {
        const result = await authenticationService.bootstrap()
        if (cancelled) {
          return
        }
        setState({
          checking: false,
          user: result.user,
          errorMessage: result.user ? null : result.error?.message ?? null,
        })
      } catch {
        if (cancelled) {
          return
        }
        setState({
          checking: false,
          user: null,
          errorMessage: null,
        })
      }
    })()

    return () => {
      cancelled = true
    }
  }, [authenticationService, attempt])

  const retryBootstrap = useCallback(() => {
    setState({ checking: true, user: null, errorMessage: null })
    setAttempt((value) => value + 1)
  }, [])

  return {
    checking: state.checking,
    restoredUser: state.user,
    bootstrapError: state.errorMessage,
    retryBootstrap,
  }
}
