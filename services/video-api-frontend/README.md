# Video API Frontend

Aplicação web independente da plataforma FIAP X para demonstrar um fluxo de login. Ela existe como uma interface de referência para a Video API, mas não se conecta a ela: a autenticação é local e mockada.

Após um login válido, a própria tela confirma a sessão autenticada. Não existem rotas protegidas, dashboard, cadastro ou recuperação de senha neste escopo.

## Tecnologias

- React 19 e TypeScript
- Vite, para desenvolvimento e build de produção
- Vitest e React Testing Library, para testes de unidade e interface
- ESLint, para análise estática

## Arquitetura

O código de autenticação é separado por responsabilidade em `src/auth`:

- `domain`: tipos e porta `AuthenticationService` consumidos pela interface.
- `application`: validação e normalização do login.
- `infrastructure`: implementação local `MockAuthenticationService`.
- `presentation`: formulário e seus estados visuais.

Essa separação permite substituir o mock por um cliente HTTP no futuro sem acoplar a tela à infraestrutura.

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

## Credenciais de demonstração

| Campo | Valor |
| --- | --- |
| E-mail | `demo@fiapx.local` |
| Senha | `MockPassword123!` |

Qualquer outra combinação apresenta a mensagem segura `E-mail ou senha inválidos.`

## Comandos disponíveis

| Comando | Finalidade |
| --- | --- |
| `npm run dev` | Inicia o servidor local com recarregamento automático. |
| `npm run lint` | Analisa o código com ESLint. |
| `npm run test` | Executa os testes uma vez, sem modo interativo. |
| `npm run build` | Faz a checagem TypeScript e gera os arquivos de produção em `dist/`. |

## Comportamento e segurança

- O e-mail tem os espaços das extremidades removidos antes da validação e autenticação.
- A senha é mantida exatamente como digitada.
- Dados inválidos impedem a chamada de autenticação e mostram erros associados a cada campo.
- Durante uma autenticação pendente, o botão fica desabilitado para evitar envios duplicados.
- Não há chamadas de rede, JWT, `localStorage`, cookies, tokens ou persistência de sessão.
- As credenciais acima são públicas, exclusivas para demonstração e não podem ser usadas em produção.

## Verificação

Antes de enviar mudanças, execute:

```bash
npm run lint
npm run test
npm run build
```
