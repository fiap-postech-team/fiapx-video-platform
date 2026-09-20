# Video API Frontend

Interface React da FIAP X para uma pessoa que envia vídeos e acompanha a extração de imagens. Há seis telas navegáveis: Entrar, Cadastrar, Meus vídeos, Detalhe do vídeo, Enviar vídeo e Meu perfil. Cadastro, entrada, renovação de sessão e saída usam o `video-api`. Lista, envio e download de vídeos ainda são simulados em memória.

A linguagem visível está em português do Brasil e documentada em [docs/prototype-copy.md](docs/prototype-copy.md). Estados internos, identificadores e contratos da API não aparecem na interface. O token de acesso fica só na memória do navegador.

## Telas

| Tela | O que a pessoa faz |
| --- | --- |
| Entrar | Acessa a conta com e-mail e senha |
| Cadastrar | Cria uma conta e volta para Entrar |
| Meus vídeos | Vê um item por arquivo, com status do vídeo e do processamento |
| Detalhe do vídeo | Consulta andamento, histórico e a ação de baixar imagens (simulada) |
| Enviar vídeo | Seleciona um arquivo e inspeciona progresso, sucesso e falha |
| Meu perfil | Consulta o e-mail da sessão, sem edição |

Depois de entrar, o menu leva a Meus vídeos, Enviar vídeo, Meu perfil e Sair. No celular o menu é recolhível.

A lista mostra só o status do vídeo (`pendente`, `enviado`, `rejeitado`, `expirado`). O detalhe mostra o status do processamento (`pendente`, `processando`, `completado`, `error`).

## Tecnologias

- React 19 e TypeScript
- Vite, Vitest, React Testing Library e ESLint

## Arquitetura

- `auth`: validação, adaptador HTTP de conta/sessão e store do token em memória
- `videos`: modelo de vídeo, estados visíveis, mock e telas de lista, detalhe e envio
- `shell` e `profile`: estrutura autenticada e consulta da conta
- `product-copy.ts`: catálogo dos textos visíveis

A porta `VideoService` permanece mockada. Chamadas autenticadas futuras devem usar `authorizedFetch`.

## Executar

O `video-api` precisa estar no ar (perfil `local`, porta 8080), com `GET /v1/me` e origens da SPA em `APP_WEB_ALLOWED_ORIGINS` / `app.web.allowed-origins`. Use **localhost** nos dois lados: cookies `SameSite=Strict` não cruzam `localhost` e `127.0.0.1`.

```bash
cd services/video-api-frontend
cp .env.example .env
npm install
npm run dev -- --host localhost --port 5173
```

URL local: [http://localhost:5173](http://localhost:5173).

Crie uma conta em Cadastrar e entre com o mesmo e-mail e senha. A senha da API tem no mínimo 12 caracteres; a interface continua validando 8 a 128 e mostra uma falha genérica se o serviço recusar o cadastro.

Formatos aceitos no envio simulado: MP4, MOV, WebM e MKV, até 500 MB (500.000.000 bytes). Nenhum byte de vídeo é enviado nem gravado no serviço.

## Verificação

```bash
npm run lint
npm run test
npm run build
```

Revisão visual da solicitante: desktop 1440 × 900 e celular 390 × 844, sem rolagem horizontal, com o inventário de textos em mãos. O aceite é dessa revisão, não de telemetria.
