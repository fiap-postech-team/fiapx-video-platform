# Video API Frontend

Protótipo web da plataforma FIAP X. A interface cobre o contrato HTTP já
exposto pela Video API: cadastro de conta `USER`, login, logout, listagem de
jobs do proprietário, criação de job a partir de uma `sourceKey` e consulta do
estado. Ela ainda não chama a API: os serviços de autenticação e de jobs são
mocks locais alinhados a `contracts/openapi.yaml`.

## Telas

| Tela | Comportamento correspondente na API |
| --- | --- |
| Cadastrar | `POST /v1/auth/register` cria conta `USER`; e-mail único; senha de 12 a 128 caracteres |
| Entrar | `POST /v1/auth/login`; credenciais inválidas não revelam se a conta existe |
| Meus jobs | `GET /v1/jobs` lista somente jobs do proprietário, com paginação por cursor |
| Novo job | `POST /v1/jobs` com `sourceKey` e `Idempotency-Key` opcional |
| Detalhe do job | `GET /v1/jobs/{id}` com estados `PENDING`, `PROCESSING`, `COMPLETED` e `FAILED` |
| Sair | `POST /v1/auth/logout` |

O cadastro não autentica sozinho: depois de `201`, a pessoa entra com as
credenciais, como a API. Upload HTTP, download do ZIP e rotas `/v1/admin/**`
não existem neste protótipo porque ainda não fazem parte do contrato público
consumível pela UI.

## Tecnologias

- React 19 e TypeScript
- Vite, para desenvolvimento e build de produção
- Vitest e React Testing Library, para testes de unidade e interface
- ESLint, para análise estática

## Arquitetura

O código continua separado por capacidade de negócio:

- `auth/domain` e `jobs/domain`: tipos e portas consumidos pela interface.
- `application`: validação alinhada ao OpenAPI (e-mail, senha, `sourceKey`, chave de idempotência).
- `infrastructure`: mocks `MockAuthenticationService` e `MockJobService`.
- `presentation`: formulários e o workspace autenticado.

Essa separação permite substituir os mocks por um cliente HTTP sem acoplar as
telas à infraestrutura.

## Pré-requisitos

É necessário Node.js 22 ou posterior e npm. Confira as versões instaladas com:

```bash
node --version
npm --version
```

## Executar o servidor de desenvolvimento

No diretório deste serviço, instale as dependências e inicie o Vite:

```bash
cd services/video-api-frontend
npm install
npm run dev
```

O terminal exibirá a URL local; normalmente é [http://localhost:5173](http://localhost:5173). Para encerrar o servidor, pressione `Ctrl+C` no mesmo terminal.

## Credenciais e source keys de demonstração

| Campo | Valor |
| --- | --- |
| E-mail | `demo@fiapx.local` |
| Senha | `MockPassword123!` |
| `sourceKey` confirmada | `videos/demo/aula-confirmada.mp4` |
| `sourceKey` não confirmada | `videos/demo/rascunho.mp4` |

A conta de demonstração já possui jobs nos quatro estados da máquina da API.
Qualquer outra combinação de login apresenta a mensagem segura `E-mail ou senha inválidos.`
Uma `sourceKey` desconhecida devolve o equivalente a 404 do proprietário.

## Comandos disponíveis

| Comando | Finalidade |
| --- | --- |
| `npm run dev` | Inicia o servidor local com recarregamento automático. |
| `npm run lint` | Analisa o código com ESLint. |
| `npm run test` | Executa os testes uma vez, sem modo interativo. |
| `npm run build` | Faz a checagem TypeScript e gera os arquivos de produção em `dist/`. |

## Comportamento e segurança

- O e-mail é normalizado (trim + minúsculas) antes da validação, como no backend.
- A senha é mantida exatamente como digitada e precisa ter entre 12 e 128 caracteres.
- Dados inválidos impedem a chamada do serviço e mostram erros associados a cada campo.
- Durante envio pendente, o botão fica desabilitado para evitar duplicidade.
- A listagem nunca mistura jobs de outro proprietário.
- Não há chamadas de rede, JWT, `localStorage`, cookies, tokens ou persistência de sessão.
- As credenciais acima são públicas, exclusivas para demonstração e não podem ser usadas em produção.

## Verificação

Antes de enviar mudanças, execute:

```bash
npm run lint
npm run test
npm run build
```
