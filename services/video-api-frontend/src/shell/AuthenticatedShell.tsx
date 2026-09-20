import { type ReactNode, useEffect, useId, useRef, useState } from 'react'
import { copy } from '../product-copy'
import { Sidebar } from './Sidebar'
import type { ProductView } from './navigation'

interface AuthenticatedShellProps {
  email: string
  view: ProductView
  children: ReactNode
  logoutPending?: boolean
  logoutError?: boolean
  onNavigate: (view: ProductView) => void
  onLogout: () => void
}

export function AuthenticatedShell({
  email,
  view,
  children,
  logoutPending = false,
  logoutError = false,
  onNavigate,
  onLogout,
}: AuthenticatedShellProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const toggleRef = useRef<HTMLButtonElement>(null)
  const menuId = useId()

  useEffect(() => {
    setMenuOpen(false)
  }, [view])

  useEffect(() => {
    if (!menuOpen) {
      return undefined
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setMenuOpen(false)
        toggleRef.current?.focus()
      }
    }

    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [menuOpen])

  function navigate(next: ProductView) {
    setMenuOpen(false)
    onNavigate(next)
  }

  return (
    <div className="app-shell">
      <header className="shell-bar">
        <button
          ref={toggleRef}
          type="button"
          className="menu-toggle"
          aria-expanded={menuOpen}
          aria-controls={menuId}
          onClick={() => setMenuOpen((open) => !open)}
        >
          {menuOpen ? copy.shell.closeMenu : copy.shell.openMenu}
        </button>
        <div className="wordmark" aria-label={copy.shell.brand}>
          FIAP <span>X</span>
        </div>
      </header>

      {menuOpen && (
        <button
          type="button"
          className="menu-backdrop"
          aria-label={copy.shell.closeMenu}
          onClick={() => {
            setMenuOpen(false)
            toggleRef.current?.focus()
          }}
        />
      )}

      <div className="shell-layout">
        <aside id={menuId} className={menuOpen ? 'shell-sidebar is-open' : 'shell-sidebar'}>
          <Sidebar
            email={email}
            view={view}
            logoutPending={logoutPending}
            onNavigate={navigate}
            onLogout={onLogout}
          />
        </aside>
        <main className="shell-main" id="conteudo-principal">
          {logoutError && (
            <div className="session-banner" role="alert">
              <p>{copy.shell.logoutUnavailable}</p>
              <button type="button" onClick={onLogout} disabled={logoutPending}>
                {logoutPending ? copy.shell.logoutPending : copy.shell.logoutRetry}
              </button>
            </div>
          )}
          {children}
        </main>
      </div>
    </div>
  )
}
