import { useState } from 'react'
import type { AuthenticatedUser, AuthenticationService } from './auth/domain/authentication'
import { MockAuthenticationService } from './auth/infrastructure/mock-authentication-service'
import { LoginForm } from './auth/presentation/LoginForm'
import { RegisterForm } from './auth/presentation/RegisterForm'
import type { JobService } from './jobs/domain/job'
import { MockJobService } from './jobs/infrastructure/mock-job-service'
import { Workspace } from './jobs/presentation/Workspace'
import './App.css'

interface AppProps {
  authenticationService?: AuthenticationService
  jobService?: JobService
}

type AccessMode = 'login' | 'register'

const defaultAuthenticationService = new MockAuthenticationService()
const defaultJobService = new MockJobService()

export default function App({
  authenticationService = defaultAuthenticationService,
  jobService = defaultJobService,
}: AppProps) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [accessMode, setAccessMode] = useState<AccessMode>('login')
  const [registeredEmail, setRegisteredEmail] = useState('')
  const [notice, setNotice] = useState<string | null>(null)

  async function handleLogout() {
    await authenticationService.logout()
    setUser(null)
    setAccessMode('login')
    setNotice(null)
  }

  function handleRegistered(email: string) {
    setRegisteredEmail(email)
    setNotice('Conta criada. Entre com o e-mail e a senha cadastrados.')
    setAccessMode('login')
  }

  if (user) {
    return <Workspace user={user} jobService={jobService} onLogout={() => { void handleLogout() }} />
  }

  return (
    <main className="page-shell">
      <section className="proof-sheet" aria-labelledby="product-title">
        <div className="wordmark" aria-label="FIAP X">FIAP <span>X</span></div>
        <p className="sheet-index">PROTOCOLO DE EXTRAÇÃO / PROTÓTIPO</p>
        <h1 id="product-title">O próximo frame do seu investimento começa aqui.</h1>
        <p className="product-summary">
          Cadastre-se, autentique a sessão e acompanhe jobs assíncronos que transformam um vídeo
          confirmado em um ZIP de frames.
        </p>
        <div className="contact-sheet" aria-label="Visão ilustrativa do fluxo de processamento de vídeo">
          <div className="source-tile"><span>SOURCE KEY</span><strong>vídeo.mp4</strong><i /></div>
          <div className="frame-strip" aria-hidden="true">
            <span /><span /><span /><span /><span /><span />
          </div>
          <div className="output-tile"><span>ENTREGA</span><strong>imagens.zip</strong><i /></div>
        </div>
        <div className="flow-notes" aria-label="Etapas da visão do produto">
          <p><b>01</b> Conta USER</p>
          <p><b>02</b> Job PENDING</p>
          <p><b>03</b> ZIP no storage</p>
        </div>
        <p className="illustrative-note">
          Protótipo alinhado à Video API atual: cadastro, login, listagem, criação e consulta de jobs.
          Upload HTTP e download do ZIP ainda estão no roadmap.
        </p>
      </section>
      <section className="access-panel" aria-labelledby="page-title">
        <div className="access-heading">
          <p>ACESSO À PLATAFORMA</p>
          <div className="access-tabs" role="tablist" aria-label="Acesso">
            <button
              type="button"
              role="tab"
              aria-selected={accessMode === 'login'}
              className={accessMode === 'login' ? 'is-active' : undefined}
              onClick={() => setAccessMode('login')}
            >
              Entrar
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={accessMode === 'register'}
              className={accessMode === 'register' ? 'is-active' : undefined}
              onClick={() => { setAccessMode('register'); setNotice(null) }}
            >
              Cadastrar
            </button>
          </div>
          {accessMode === 'login' ? (
            <>
              <h2 id="page-title">Entre para acompanhar a extração.</h2>
              <p>Use a conta USER local. A sessão do protótipo espelha o contrato de login da API.</p>
            </>
          ) : (
            <>
              <h2 id="page-title">Crie uma conta USER.</h2>
              <p>O cadastro público não concede ADMIN. E-mail único, senha de 12 a 128 caracteres.</p>
            </>
          )}
        </div>
        {notice && accessMode === 'login' && <p className="notice" role="status">{notice}</p>}
        {accessMode === 'login' ? (
          <LoginForm
            authenticationService={authenticationService}
            onAuthenticated={setUser}
            initialEmail={registeredEmail}
          />
        ) : (
          <RegisterForm authenticationService={authenticationService} onRegistered={handleRegistered} />
        )}
        <p className="access-footnote">
          Protótipo local. Não envia JWT, cookies nem chamadas à API; os contratos HTTP estão
          representados nas telas e nos mocks.
        </p>
      </section>
    </main>
  )
}
