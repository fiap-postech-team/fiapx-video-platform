# Video API Frontend

Protótipo React da FIAP X para uma pessoa que envia vídeos e acompanha a extração de imagens. Há seis telas navegáveis: Entrar, Cadastrar, Meus vídeos, Detalhe do vídeo, Enviar vídeo e Meu perfil. Autenticação, lista, envio e download são simulados em memória. Não há chamadas HTTP.

A linguagem visível está em português do Brasil e documentada em [docs/prototype-copy.md](docs/prototype-copy.md). Estados internos, identificadores e contratos da API não aparecem na interface.

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

- `auth`: validação e mock de cadastro/entrada
- `videos`: modelo de vídeo, estados visíveis, mock e telas de lista, detalhe e envio
- `shell` e `profile`: estrutura autenticada e consulta da conta
- `prototype`: controles de cenário
- `product-copy.ts`: catálogo dos textos visíveis

A porta `VideoService` existe para uma futura troca do mock por HTTP. Este épico não consome a API.

## Executar

```bash
cd services/video-api-frontend
npm install
npm run dev
```

URL local: [http://localhost:5173](http://localhost:5173).

| Campo | Valor |
| --- | --- |
| E-mail | `demo@fiapx.local` |
| Senha | `MockPassword123!` |

Formatos aceitos no envio simulado: MP4, MOV, WebM e MKV, até 500 MB (500.000.000 bytes). Nenhum byte é enviado nem gravado no serviço.

## Verificação

```bash
npm run lint
npm run test
npm run build
```

Revisão visual da solicitante: desktop 1440 × 900 e celular 390 × 844, sem rolagem horizontal, com o inventário de textos em mãos. O aceite é dessa revisão, não de telemetria.
