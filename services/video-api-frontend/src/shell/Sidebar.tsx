import { copy } from '../product-copy'
import { isDetailView, sectionOf, type ProductView } from './navigation'

interface SidebarProps {
  email: string
  view: ProductView
  logoutPending?: boolean
  onNavigate: (view: ProductView) => void
  onLogout: () => void
}

export function Sidebar({ email, view, logoutPending = false, onNavigate, onLogout }: SidebarProps) {
  const section = sectionOf(view)
  const videosCurrent = section === 'videos' || isDetailView(view)

  return (
    <>
      <div className="sidebar-brand">
        <p className="wordmark" aria-label={copy.shell.brand}>FIAP <span>X</span></p>
      </div>
      <nav className="sidebar-nav" aria-label={copy.shell.mainNav}>
        <button
          type="button"
          className={videosCurrent ? 'is-current' : undefined}
          aria-current={videosCurrent ? 'page' : undefined}
          onClick={() => onNavigate('videos')}
        >
          {copy.shell.navVideos}
        </button>
        <button
          type="button"
          className={section === 'upload' ? 'is-current' : undefined}
          aria-current={section === 'upload' ? 'page' : undefined}
          onClick={() => onNavigate('upload')}
        >
          {copy.shell.navUpload}
        </button>
        <button
          type="button"
          className={section === 'profile' ? 'is-current' : undefined}
          aria-current={section === 'profile' ? 'page' : undefined}
          onClick={() => onNavigate('profile')}
        >
          {copy.shell.navProfile}
        </button>
      </nav>
      <div className="sidebar-session">
        <p className="session-email">{email}</p>
        <button type="button" className="ghost" onClick={onLogout} disabled={logoutPending}>
          {logoutPending ? copy.shell.logoutPending : copy.shell.logout}
        </button>
      </div>
    </>
  )
}
