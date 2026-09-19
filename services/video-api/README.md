# video-api

Backend Spring Boot da plataforma FIAP X. Ele mantém identidades e sessões
locais, protege as rotas HTTP com JWT e persiste os dados de vídeos e jobs no
PostgreSQL. O contrato HTTP canônico é o
[OpenAPI](../../contracts/openapi.yaml).

## Estado atual

| Recurso | Situação |
|---|---|
| Cadastro público de usuário | Implementado em `POST /v1/auth/register`; sempre cria o papel `USER` |
| Login, refresh e logout | Implementados; refresh token rotativo fica em cookie HttpOnly e refresh/logout exigem CSRF |
| JWT | Emitido com `RS256`; no perfil `local`, usa par RSA efêmero e validade padrão de 15 minutos |
| Jobs | Criação, consulta do proprietário, paginação por cursor e idempotência implementadas |
| Persistência | Flyway executa as migrations `V1`, `V2` e `V3`; Hibernate apenas valida o schema |
| Swagger | Disponível somente com o perfil `local` |
| Upload/download de vídeo | Ainda não há endpoint HTTP; um job exige um vídeo já confirmado no banco |
| Publicação/consumo RabbitMQ | Schema, outbox, inbox e operações de claim existem; publisher e listener integrados ao broker ainda não estão disponíveis |

## Pré-requisitos

- JDK 21;
- Docker Desktop ou Docker Engine em execução, para o PostgreSQL local;
- Maven Wrapper do repositório (não é necessário instalar Maven).

As portas `5432` (PostgreSQL) e `8080` (API) devem estar livres.

## Início rápido: testar cadastro de usuário

Os passos abaixo sobem somente a dependência necessária ao cadastro. Execute-os
na raiz do monorepo.

### 1. Inicie o PostgreSQL

```bash
docker compose up -d postgres
docker compose ps postgres
```

Espere o estado `healthy`. O Compose usa, no ambiente local, banco `fiapx`,
usuário `fiapx` e senha `fiapx`.

### 2. Inicie a API no perfil local

Em outro terminal:

```bash
./mvnw -pl services/video-api -am spring-boot:run
```

O goal `spring-boot:run` usa o perfil `local` por padrão. Ele fornece as
credenciais locais do banco e gera um par RSA
efêmero. Por isso, os tokens emitidos deixam de valer quando a aplicação é
reiniciada. Não use esse perfil nem essas credenciais em ambiente compartilhado.

Para usar outro perfil nesse goal, substitua o valor padrão explicitamente:

```bash
./mvnw -pl services/video-api -am spring-boot:run -Dapp.profiles=staging
```

### 3. Confirme que a API está disponível

```bash
curl --fail-with-body http://localhost:8080/actuator/health
```

O retorno esperado é um JSON com `"status":"UP"`.

### 4. Cadastre um usuário

```bash
curl --include \
  --request POST http://localhost:8080/v1/auth/register \
  --header 'Content-Type: application/json' \
  --data '{"email":"teste.usuario@example.test","password":"senha-de-teste-123"}'
```

Resposta esperada: `201 Created`.

```json
{
  "id": "<uuid>",
  "email": "teste.usuario@example.test",
  "roles": ["USER"]
}
```

O e-mail é removido de espaços nas pontas e normalizado para minúsculas. A senha
deve ter entre 12 e 128 caracteres; ela nunca é devolvida nem gravada em texto
claro.

### 5. Exercite os casos mínimos

Repita o mesmo cadastro para verificar a proteção contra duplicidade. O retorno
esperado é `409 Conflict` com `Content-Type: application/problem+json`.

```bash
curl --include \
  --request POST http://localhost:8080/v1/auth/register \
  --header 'Content-Type: application/json' \
  --data '{"email":"teste.usuario@example.test","password":"senha-de-teste-123"}'
```

Para validar a regra de senha, envie uma senha curta. O retorno esperado é
`400 Bad Request`.

```bash
curl --include \
  --request POST http://localhost:8080/v1/auth/register \
  --header 'Content-Type: application/json' \
  --data '{"email":"outro.usuario@example.test","password":"curta"}'
```

### 6. Pare o ambiente quando terminar

Interrompa a API com `Ctrl+C` e execute:

```bash
docker compose stop postgres
```

O comando preserva o volume do banco. Para reiniciar os testes de dados do
zero, remova explicitamente o volume `postgres-data` depois de confirmar que
nenhum dado local precisa ser preservado.

## Autenticação e sessões

| Rota | Acesso | Resultado |
|---|---|---|
| `POST /v1/auth/register` | Público | Cria uma conta `USER` e retorna `201` |
| `POST /v1/auth/login` | Público | Retorna access token e envia `FIAPX_REFRESH` e `XSRF-TOKEN` em cookies |
| `POST /v1/auth/refresh` | Cookie + header `X-XSRF-TOKEN` | Rotaciona o refresh token e retorna novo access token |
| `POST /v1/auth/logout` | Cookie + header `X-XSRF-TOKEN` | Revoga a sessão e expira os cookies |

O login bloqueia a conta por 15 minutos após cinco falhas consecutivas. Os
valores podem ser alterados por `APP_AUTH_LOCK_DURATION` e
`APP_AUTH_MAX_FAILURES`. O primeiro `ADMIN` não é criado pelo cadastro público:
ele depende do bootstrap explicitamente habilitado e de segredo externo.

## Configuração

| Variável | Perfil `local` | Fora de `local` |
|---|---|---|
| `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` | `jdbc:postgresql://localhost:5432/fiapx`, `fiapx`, `fiapx` | Obrigatórias; defaults locais são rejeitados |
| `APP_AUTH_ISSUER`, `APP_AUTH_AUDIENCE`, `APP_AUTH_KEY_ID` | Valores locais | Obrigatórias |
| `APP_AUTH_PRIVATE_KEY_BASE64`, `APP_AUTH_PUBLIC_KEY_BASE64` | Par RSA efêmero | Obrigatórias, respectivamente PKCS#8 e X.509 em Base64 |
| `APP_AUTH_ACCESS_TOKEN_TTL` | `15m` | Opcional |
| `APP_AUTH_REFRESH_TOKEN_TTL` | `7d` | Opcional |
| `APP_AUTH_BOOTSTRAP_ENABLED` | `false` | Opcional; exige e-mail e senha quando `true` |

Fora do perfil `local`, inicialização sem banco ou configuração de autenticação
completa falha antes de expor a aplicação.

## Rotas locais e documentação

Com o goal local `spring-boot:run`:

| Recurso | URL |
|---|---|
| Health | `http://localhost:8080/actuator/health` |
| OpenAPI canônico empacotado | `http://localhost:8080/openapi.yaml` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |

O Swagger está em modo de consulta (`try it out` desabilitado); use os comandos
acima, Postman ou outro cliente HTTP para executar o cadastro.

## Persistência e limites atuais

As migrations `V2` e `V3` mantêm usuários, credenciais com hash BCrypt,
papéis, sessões, refresh tokens, vídeos, jobs, histórico, inbox, outbox e
idempotência. Detalhes de tabelas e compatibilidade estão no
[DER](../../docs/architecture/video-api-database.md) e a decisão de identidade
local está no [ADR 0011](../../docs/adr/0011-local-identity-with-rsa-jwt-and-oidc-boundary.md).

O endpoint de criação de job aceita `sourceKey`, mas só funciona quando existe
um vídeo confirmado daquele proprietário. Como a API ainda não expõe o ciclo de
upload/confirmação, o fluxo manual suportado hoje começa pelo cadastro e login.

## Verificação

```bash
./mvnw -pl services/video-api -am verify
```

Em 17/09/2026, os testes executáveis passaram, mas o comando encerrou com falha
no gate JaCoCo: cobertura de linhas de `0.62`, abaixo do mínimo configurado de
`0.70`. Os testes que dependem de PostgreSQL via Testcontainers requerem Docker
em execução; sem ele, são ignorados. Portanto, trate o resultado atual como
verificação parcial até que o ambiente Docker esteja ativo e a cobertura seja
elevada.
