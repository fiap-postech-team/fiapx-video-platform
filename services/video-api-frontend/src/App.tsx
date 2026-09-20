import { useState } from 'react'
import type { AuthenticatedUser, AuthenticationService } from './auth/domain/authentication'
import { MockAuthenticationService } from './auth/infrastructure/mock-authentication-service'
import { LoginForm } from './auth/presentation/LoginForm'
import { RegisterForm } from './auth/presentation/RegisterForm'
import { copy } from './product-copy'
import { Profile } from './profile/Profile'
import { AuthenticatedShell } from './shell/AuthenticatedShell'
import { isDetailView, type ProductView } from './shell/navigation'
import type { ScenarioKind, VideoService } from './videos/domain/video'
import { MockVideoService } from './videos/infrastructure/mock-video-service'
import { UploadVideo } from './videos/presentation/UploadVideo'
import { VideoDetail } from './videos/presentation/VideoDetail'
import { VideoList } from './videos/presentation/VideoList'
import './App.css'

interface AppProps {
  authenticationService?: AuthenticationService
  videoService?: VideoService
}

type AccessMode = 'login' | 'register'

const defaultAuthenticationService = new MockAuthenticationService()
const defaultVideoService = new MockVideoService()

export default function App({
  authenticationService = defaultAuthenticationService,
  videoService = defaultVideoService,
}: AppProps) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [accessMode, setAccessMode] = useState<AccessMode>('login')
  const [registeredEmail, setRegisteredEmail] = useState('')
  const [notice, setNotice] = useState<string | null>(null)
  const [view, setView] = useState<ProductView>('videos')
  const [scenario, setScenario] = useState<ScenarioKind>('default')

  function navigate(next: ProductView) {
    setView(next)
  }

  async function handleLogout() {
    await authenticationService.logout()
    setUser(null)
    setAccessMode('login')
    setNotice(null)
    setView('videos')
    setScenario('default')
  }

  function handleRegistered(email: string) {
    setRegisteredEmail(email)
    setNotice(copy.access.registerSuccess)
    setAccessMode('login')
  }

  if (user) {
    const scenarioValue = { kind: scenario }
    return (
      <AuthenticatedShell
        email={user.email}
        view={view}
        onNavigate={navigate}
        onLogout={() => { void handleLogout() }}
      >
        {view === 'videos' && (
          <VideoList
            userId={user.id}
            videoService={videoService}
            scenario={scenarioValue}
            onOpen={(videoId) => navigate({ kind: 'video-detail', videoId })}
            onUpload={() => navigate('upload')}
          />
        )}
        {isDetailView(view) && (
          <VideoDetail
            userId={user.id}
            videoId={view.videoId}
            videoService={videoService}
            onBack={() => navigate('videos')}
          />
        )}
        {view === 'upload' && (
          <UploadVideo
            userId={user.id}
            videoService={videoService}
            scenario={scenarioValue}
            onFinished={() => navigate('videos')}
          />
        )}
        {view === 'profile' && <Profile user={user} />}
      </AuthenticatedShell>
    )
  }

  return (
    <main className="page-shell">
      <section className="proof-sheet" aria-labelledby="product-title">
        <div className="wordmark" aria-label={copy.shell.brand}>FIAP <span>X</span></div>
        <p className="sheet-index">{copy.access.kicker}</p>
        <h1 id="product-title">{copy.access.heroTitle}</h1>
        <p className="product-summary">{copy.access.heroLead}</p>
        <div className="contact-sheet" aria-label={copy.access.heroLead}>
          <div className="source-tile"><span>{copy.access.stepSend}</span><strong>vídeo</strong><i /></div>
          <div className="frame-strip" aria-hidden="true">
            <span /><span /><span /><span /><span /><span />
          </div>
          <div className="output-tile"><span>{copy.access.stepDownload}</span><strong>imagens</strong><i /></div>
        </div>
        <div className="flow-notes">
          <p><b>01</b> {copy.access.stepSend}</p>
          <p><b>02</b> {copy.access.stepProcess}</p>
          <p><b>03</b> {copy.access.stepDownload}</p>
        </div>
      </section>
      <section className="access-panel" aria-labelledby="page-title">
        <div className="access-heading">
          <p>{copy.access.kicker}</p>
          <div className="access-tabs" role="tablist" aria-label={copy.access.kicker}>
            <button
              type="button"
              role="tab"
              aria-selected={accessMode === 'login'}
              className={accessMode === 'login' ? 'is-active' : undefined}
              onClick={() => setAccessMode('login')}
            >
              {copy.access.enterTab}
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={accessMode === 'register'}
              className={accessMode === 'register' ? 'is-active' : undefined}
              onClick={() => { setAccessMode('register'); setNotice(null) }}
            >
              {copy.access.createTab}
            </button>
          </div>
          {accessMode === 'login' ? (
            <>
              <h2 id="page-title">{copy.access.loginTitle}</h2>
              <p>{copy.access.loginLead}</p>
            </>
          ) : (
            <h2 id="page-title">{copy.access.registerTitle}</h2>
          )}
        </div>
        {notice && accessMode === 'login' && <p className="notice" role="status">{notice}</p>}
        {accessMode === 'login' ? (
          <LoginForm
            authenticationService={authenticationService}
            onAuthenticated={(next) => { setUser(next); setView('videos') }}
            initialEmail={registeredEmail}
          />
        ) : (
          <RegisterForm authenticationService={authenticationService} onRegistered={handleRegistered} />
        )}
      </section>
    </main>
  )
}
