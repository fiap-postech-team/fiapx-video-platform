import { useState } from 'react'
import type { AuthenticatedUser, AuthenticationService } from './auth/domain/authentication'
import { MockAuthenticationService } from './auth/infrastructure/mock-authentication-service'
import { LoginForm } from './auth/presentation/LoginForm'
import './App.css'

interface AppProps {
  authenticationService?: AuthenticationService
}

const defaultAuthenticationService = new MockAuthenticationService()

export default function App({ authenticationService = defaultAuthenticationService }: AppProps) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null)

  return (
    <main className="page-shell">
      <section className="proof-sheet" aria-labelledby="product-title">
        <div className="wordmark" aria-label="FIAP X">FIAP <span>X</span></div>
        <p className="sheet-index">PROTOCOLO DE EXTRAÇÃO / DEMONSTRAÇÃO</p>
        <h1 id="product-title">O próximo frame do seu investimento começa aqui.</h1>
        <p className="product-summary">
          FIAP X está evoluindo a forma de transformar um vídeo em um pacote ZIP de imagens, pronto para download.
        </p>
        <div className="contact-sheet" aria-label="Visão ilustrativa do fluxo de processamento de vídeo">
          <div className="source-tile"><span>UPLOAD</span><strong>vídeo.mp4</strong><i /></div>
          <div className="frame-strip" aria-hidden="true">
            <span /><span /><span /><span /><span /><span />
          </div>
          <div className="output-tile"><span>ENTREGA</span><strong>imagens.zip</strong><i /></div>
        </div>
        <div className="flow-notes" aria-label="Etapas da visão do produto">
          <p><b>01</b> Envie o vídeo</p>
          <p><b>02</b> Extraia os frames</p>
          <p><b>03</b> Baixe o ZIP</p>
        </div>
        <p className="illustrative-note">Fluxo ilustrativo da evolução do produto.</p>
      </section>
      <section className="access-panel" aria-labelledby="page-title">
        <div className="access-heading">
          <p>ACESSO À DEMONSTRAÇÃO</p>
          {user ? (
            <div className="authenticated" role="status">
              <h2 id="page-title">Sessão confirmada.</h2>
              <p>Você está autenticado como <strong>{user.name}</strong>.</p>
            </div>
          ) : (
            <>
              <h2 id="page-title">Entre para acompanhar a evolução.</h2>
              <p>Use suas credenciais para acessar a demonstração FIAP X.</p>
            </>
          )}
        </div>
        {!user && <LoginForm authenticationService={authenticationService} onAuthenticated={setUser} />}
        <p className="access-footnote">Ambiente local de demonstração. Nenhuma sessão é armazenada.</p>
      </section>
    </main>
  )
}
