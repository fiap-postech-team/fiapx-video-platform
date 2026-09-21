import { useEffect, useState } from 'react'
import { useSessionBootstrap } from './auth/application/use-session-bootstrap'
import type { AuthenticatedUser, AuthenticationService } from './auth/domain/authentication'
import { AuthenticationFailure } from './auth/domain/authentication'
import { HttpAuthenticationService } from './auth/infrastructure/http-authentication-service'
import { LoginForm } from './auth/presentation/LoginForm'
import { RegisterForm } from './auth/presentation/RegisterForm'
import { copy } from './product-copy'
import { Profile } from './profile/Profile'
import { AuthenticatedShell } from './shell/AuthenticatedShell'
import { isDetailView, type ProductView } from './shell/navigation'
import type { ScenarioKind, VideoService } from './videos/domain/video'
import { MockVideoService } from './videos/infrastructure/mock-video-service'
import { VideoHttpService } from './videos/infrastructure/video-http-service'
import { UploadVideo } from './videos/presentation/UploadVideo'
import { VideoDetail } from './videos/presentation/VideoDetail'
import { VideoList } from './videos/presentation/VideoList'
import './App.css'

interface AppProps {
  authenticationService?: AuthenticationService
  videoService?: VideoService
}

type AccessMode = 'login' | 'register'

function createDefaultVideoService(auth: AuthenticationService): VideoService {
  const library = new VideoHttpService((input, init) => auth.authorizedFetch(input, init))
  const upload = new MockVideoService()
  return {
    list: (page, options) => library.list(page, options),
    get: (ref) => library.get(ref),
    simulateUpload: (userId, selection, options) => upload.simulateUpload(userId, selection, options),
  }
}

const defaultAuthenticationService = new HttpAuthenticationService()
const defaultVideoService = createDefaultVideoService(defaultAuthenticationService)

export default function App({
  authenticationService = defaultAuthenticationService,
  videoService = defaultVideoService,
}: AppProps) {
  const { checking, restoredUser, bootstrapError, retryBootstrap } = useSessionBootstrap(authenticationService)
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [accessMode, setAccessMode] = useState<AccessMode>('login')
  const [registeredEmail, setRegisteredEmail] = useState('')
  const [notice, setNotice] = useState<string | null>(null)
  const [view, setView] = useState<ProductView>('videos')
  const [scenario, setScenario] = useState<ScenarioKind>('default')
  const [logoutError, setLogoutError] = useState(false)
  const [logoutPending, setLogoutPending] = useState(false)
  const [bootstrapApplied, setBootstrapApplied] = useState(false)

  useEffect(() => {
    if (checking) {
      setBootstrapApplied(false)
      return
    }
    setUser(restoredUser)
    if (!restoredUser && bootstrapError) {
      setNotice(bootstrapError)
    }
    setBootstrapApplied(true)
  }, [bootstrapError, checking, restoredUser])

  function navigate(next: ProductView) {
    setView(next)
  }

  function resetSession(nextNotice: string | null = null) {
    setUser(null)
    setAccessMode('login')
    setNotice(nextNotice)
    setView('videos')
    setScenario('default')
    setLogoutError(false)
    setLogoutPending(false)
  }

  async function handleLogout() {
    if (logoutPending) {
      return
    }
    setLogoutPending(true)
    try {
      await authenticationService.logout()
      resetSession()
    } catch (error) {
      if (error instanceof AuthenticationFailure && error.error.code === 'SESSION_EXPIRED') {
        resetSession(copy.access.sessionEnded)
        return
      }
      setLogoutError(true)
      setLogoutPending(false)
    }
  }

  function handleRegistered(email: string) {
    setRegisteredEmail(email)
    setNotice(copy.access.registerSuccess)
    setAccessMode('login')
  }

  if (checking || !bootstrapApplied) {
    return (
      <main className="session-check">
        <p role="status">{copy.access.checkingSession}</p>
      </main>
    )
  }

  if (user) {
    const scenarioValue = { kind: scenario }
    return (
      <AuthenticatedShell
        email={user.email}
        view={view}
        logoutPending={logoutPending}
        logoutError={logoutError}
        onNavigate={navigate}
        onLogout={() => { void handleLogout() }}
      >
        {view === 'videos' && (
          <VideoList
            videoService={videoService}
            scenario={scenarioValue}
            onOpen={(videoRef) => navigate({ kind: 'video-detail', videoRef })}
            onUpload={() => navigate('upload')}
          />
        )}
        {isDetailView(view) && (
          <VideoDetail
            videoRef={view.videoRef}
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
        {notice && accessMode === 'login' && (
          <p className="notice" role="status">{notice}</p>
        )}
        {bootstrapError && accessMode === 'login' && notice === bootstrapError && (
          <button type="button" className="text-link" onClick={retryBootstrap}>
            {copy.access.retrySessionCheck}
          </button>
        )}
        {accessMode === 'login' ? (
          <LoginForm
            authenticationService={authenticationService}
            onAuthenticated={(next) => {
              setUser(next)
              setView('videos')
              setNotice(null)
              setLogoutError(false)
            }}
            initialEmail={registeredEmail}
          />
        ) : (
          <RegisterForm authenticationService={authenticationService} onRegistered={handleRegistered} />
        )}
      </section>
    </main>
  )
}
