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
| Persistência | Flyway executa as migrations `V1` a `V4`; Hibernate apenas valida o schema |
| Swagger | Disponível somente com o perfil `local` |
| Upload de vídeo | URL temporária para PUT direto no MinIO/S3, confirmação por HEAD e expiração de pendências |
| Download de vídeo | Ainda não há endpoint HTTP |
| Publicação/consumo RabbitMQ | Schema, outbox, inbox e operações de claim existem; publisher e listener integrados ao broker ainda não estão disponíveis |

## Pré-requisitos

- JDK 21;
- Docker Desktop ou Docker Engine em execução, para PostgreSQL e MinIO locais;
- Maven Wrapper do repositório (não é necessário instalar Maven).

As portas `5432` (PostgreSQL), `8080` (API) e `9000` (MinIO) devem estar livres.

## Início rápido: testar cadastro de usuário

Os passos abaixo sobem somente a dependência necessária ao cadastro. Execute-os
na raiz do monorepo.

### 1. Inicie o PostgreSQL

```bash
docker compose up -d postgres minio minio-init
docker compose ps postgres minio
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
docker compose stop postgres minio
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

## Upload direto de vídeo

Com um bearer token válido, chame `POST /v1/videos/uploads`:

```json
{"originalFilename":"clip.mp4","contentType":"video/mp4","sizeBytes":12345,"checksumSha256":null}
```

A resposta `201` contém `videoId`, `sourceKey`, `uploadUrl`, `expiresAt`,
`method: "PUT"` e `headers`. Envie os bytes diretamente à `uploadUrl` usando
exatamente o método e os headers retornados; o arquivo nunca passa pela API.
Se informar SHA-256, use 64 caracteres hexadecimais; a resposta exigirá também
o header de checksum codificado em Base64. Não persista, registre nem compartilhe
a URL pré-assinada. Ela vence em 15 minutos e o header condicional impede
sobrescrever um objeto já enviado.

Depois do PUT, chame `POST /v1/videos/{videoId}/confirm` com o mesmo bearer.
A API confere o objeto por HEAD e devolve `videoId`, `sourceKey`, `status:
"UPLOADED"` e `uploadedAt`. Repetir a confirmação devolve o mesmo resultado.
O job usa a `sourceKey` confirmada em `POST /v1/jobs`. Objeto ausente, metadados
divergentes ou upload expirado não originam job; solicite um novo upload para
corrigir um objeto incompatível.

Por padrão, a allowlist aceita `video/mp4`, `video/quicktime`, `video/webm` e
`video/x-matroska`; o tamanho declarado e o observado não podem ultrapassar
500.000.000 bytes. Pendências vencem em 24 horas; a limpeza agendada marca
`EXPIRED` e remove o objeto, repetindo falhas de storage. Formato real e duração
máxima de 30 minutos são responsabilidade do `video-processor` em entrega
separada. Para clientes de navegador, configure o CORS do bucket para permitir
PUT com `Content-Type`, `If-None-Match` e `x-amz-checksum-sha256` a partir da
origem autorizada.

## Configuração

| Variável | Perfil `local` | Fora de `local` |
|---|---|---|
| `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` | `jdbc:postgresql://localhost:5432/fiapx`, `fiapx`, `fiapx` | Obrigatórias; defaults locais são rejeitados |
| `APP_AUTH_ISSUER`, `APP_AUTH_AUDIENCE`, `APP_AUTH_KEY_ID` | Valores locais | Obrigatórias |
| `APP_AUTH_PRIVATE_KEY_BASE64`, `APP_AUTH_PUBLIC_KEY_BASE64` | Par RSA efêmero | Obrigatórias, respectivamente PKCS#8 e X.509 em Base64 |
| `APP_AUTH_ACCESS_TOKEN_TTL` | `15m` | Opcional |
| `APP_AUTH_REFRESH_TOKEN_TTL` | `7d` | Opcional |
| `APP_AUTH_BOOTSTRAP_ENABLED` | `false` | Opcional; exige e-mail e senha quando `true` |
| `S3_BUCKET`, `S3_REGION` | `videos`, `us-east-1` | Bucket e região do storage |
| `S3_ENDPOINT`, `S3_PUBLIC_ENDPOINT` | `http://localhost:9000` | Endpoint interno para HEAD/DELETE e endpoint alcançável pelo cliente para a URL assinada; sem override usa S3 padrão |
| `S3_ACCESS_KEY`, `S3_SECRET_KEY` | Usuário local `fiapx-video-api` com política restrita | Omitir ambos para usar a cadeia de credenciais do ambiente; fornecer ambos quando usar chaves explícitas |
| `APP_VIDEO_MAX_SIZE_BYTES`, `APP_VIDEO_ALLOWED_CONTENT_TYPES` | `500000000`, quatro tipos acima | Política de mídia declarada |
| `APP_VIDEO_UPLOAD_URL_TTL`, `APP_VIDEO_PENDING_TTL` | `PT15M`, `PT24H` | Duração da URL e da pendência; a pendência deve durar mais |
| `APP_VIDEO_CLEANUP_INTERVAL`, `APP_VIDEO_CLEANUP_BATCH_SIZE` | `PT5M`, `100` | Agendamento e tamanho máximo de cada rodada |

Fora do perfil `local`, inicialização sem banco ou configuração de autenticação
completa falha antes de expor a aplicação.
Em produção, use credenciais exclusivas da API com `PutObject`, `GetObject`
(HEAD) e `DeleteObject` restritos ao prefixo `users/*/videos/*/source`, mais
`ListBucket` restrito ao prefixo de entrada para distinguir objeto ausente.
O Compose cria essa política e um usuário local dedicado; o processor mantém
outra identidade.
No Compose, `S3_ENDPOINT` aponta para `minio:9000` e `S3_PUBLIC_ENDPOINT` para
`localhost:9000`, preservando o host que participa da assinatura.

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

As migrations `V2` a `V4` mantêm usuários, credenciais com hash BCrypt,
papéis, sessões, refresh tokens, vídeos, jobs, histórico, inbox, outbox e
idempotência. Detalhes de tabelas e compatibilidade estão no
[DER](../../docs/architecture/video-api-database.md) e a decisão de identidade
local está no [ADR 0011](../../docs/adr/0011-local-identity-with-rsa-jwt-and-oidc-boundary.md).

O endpoint de criação de job aceita `sourceKey` somente quando existe vídeo
confirmado daquele proprietário. A API não fornece URL de download neste MVP.

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
