# Video API Frontend

Interface React da FIAP X para uma pessoa que envia vídeos e acompanha a extração de imagens. Há seis telas navegáveis: Entrar, Cadastrar, Meus vídeos, Detalhe do vídeo, Enviar vídeo e Meu perfil. Cadastro, entrada, renovação de sessão, saída, upload, processamento, lista e detalhe usam o `video-api`.

A linguagem visível está em português do Brasil e documentada em [docs/prototype-copy.md](docs/prototype-copy.md). Estados internos, identificadores e contratos da API não aparecem na interface. O token de acesso fica só na memória do navegador.

## Telas

| Tela | O que a pessoa faz |
| --- | --- |
| Entrar | Acessa a conta com e-mail e senha |
| Cadastrar | Cria uma conta e volta para Entrar |
| Meus vídeos | Vê um item por arquivo, com um status de produto e páginas numeradas |
| Detalhe do vídeo | Consulta o andamento do envio e do único processamento |
| Enviar vídeo | Seleciona um arquivo e inspeciona progresso, sucesso e falha |
| Meu perfil | Consulta o e-mail da sessão, sem edição |

Depois de entrar, o menu leva a Meus vídeos, Enviar vídeo, Meu perfil e Sair. No celular o menu é recolhível.

A lista mostra um status de produto por arquivo (Pendente, Processando, Processado, Rejeitado, Expirado, Falha no processamento), com 5 itens por página. Para vídeos Processados, a lista e o detalhe exibem `Baixar resultado`; a API emite uma URL temporária e a interface a abre em nova aba sem persistir a URL.

## Tecnologias

- React 19 e TypeScript
- Vite, Vitest, React Testing Library e ESLint

## Arquitetura

- `auth`: validação, adaptador HTTP de conta/sessão e store do token em memória
- `videos`: modelo de vídeo, adaptador HTTP, estados visíveis e telas de lista, detalhe e envio
- `shell` e `profile`: estrutura autenticada e consulta da conta
- `product-copy.ts`: catálogo dos textos visíveis

As chamadas JSON usam `authorizedFetch`. O arquivo é enviado diretamente para a URL temporária retornada pela API, sem bearer ou cookie, e essa URL fica somente em memória.

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

Formatos aceitos: MP4, MOV, WebM e MKV, com arquivo não vazio e até 500 MB (500.000.000 bytes). A pessoa confirma a decisão antes da reserva; depois, a tela mostra o progresso real do `PUT`, confirma o objeto e acompanha o job até “Concluído” ou “Falhou”. O `POST /v1/jobs` sai só com bearer e `Content-Type`. A interface não envia `Idempotency-Key`: o CORS local não inclui esse cabeçalho e o navegador bloquearia a chamada. Um processamento por vídeo continua garantido no serviço. No perfil local, o backend ainda usa armazenamento e resultado demonstrativos, sem indicar isso na interface.

## Verificação

```bash
npm run lint
npm run test
npm run build
```

Revisão visual da solicitante: desktop 1440 × 900 e celular 390 × 844, sem rolagem horizontal, com o inventário de textos em mãos. O aceite é dessa revisão, não de telemetria.
