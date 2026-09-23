# video-api

API HTTP da plataforma FIAP X, desenvolvida em Java 21 e Spring Boot. É
responsável por cadastro e autenticação de usuários, upload direto de vídeos
para S3/MinIO, criação e consulta de jobs de processamento.

## Arquitetura

O serviço é organizado por capacidades (`identity`, `videos`, `jobs`, `outbox` e
`inbox`). PostgreSQL mantém identidades, metadados dos vídeos, jobs e eventos;
os arquivos ficam no object storage. O cliente envia o vídeo diretamente ao
S3/MinIO por URL pré-assinada e confirma o upload antes de criar um job.
A biblioteca do proprietário é consultada em `GET /v1/videos` e
`GET /v1/videos/{videoRef}` (Flyway `V5`).

Na criação do job, o estado `PENDING`, o histórico inicial e a intenção de
publicar `video.job.requested.v1` são persistidos na mesma transação. O publisher
RabbitMQ processa a outbox em background, com retry e publisher confirms. O
processamento de mídia pertence ao `video-processor`, não à API. Cada vídeo
possui no máximo um job; uma nova `Idempotency-Key` não substitui esse bloqueio.
Resultados `started`, `completed` e `failed` são consumidos pela fila durável
`video.api.results.v1`, com inbox e atualização do job na mesma transação.

Contratos: [OpenAPI](../../contracts/openapi.yaml) e
[AsyncAPI](../../contracts/asyncapi.yaml).

## Como executar localmente

Pré-requisitos: JDK 21, Docker e portas `5432`, `5672`, `8080` e `9000` livres. Na raiz
do monorepo:

```bash
docker compose up -d postgres rabbitmq minio minio-init
./mvnw -pl services/video-api -am spring-boot:run
```

RabbitMQ local: `amqp://fiapx:fiapx@localhost:5672`; painel de administração:
`http://localhost:15672` (usuário e senha `fiapx`). O video-api também inicia
quando o broker está indisponível; os eventos pendentes serão enviados após a
recuperação.

O limite máximo da listagem de jobs pode ser configurado por
`VIDEO_API_JOBS_MAX_PAGE_SIZE` (padrão `100`). O consumidor de resultados pode
ser desabilitado em testes com `VIDEO_API_JOBS_RESULT_LISTENER_ENABLED=false`.

No profile `local`, jobs `COMPLETED` com `resultKey` presente têm um ZIP pequeno
materializado automaticamente no storage mockado para permitir testes com
qualquer proprietário. Essa preparação não exige variáveis de ambiente.

Para criar ou reutilizar também um usuário e vídeo de demonstração, habilite a
fixture com credenciais exclusivas do ambiente local:

```bash
export APP_VIDEO_DOWNLOAD_FIXTURE_ENABLED=true
export APP_VIDEO_DOWNLOAD_FIXTURE_EMAIL=download-demo@fiapx.local
export APP_VIDEO_DOWNLOAD_FIXTURE_PASSWORD='SenhaLocalSegura123'
./mvnw -pl services/video-api -am spring-boot:run
```

A fixture cria ou reutiliza `demo-processado.mp4`, um job `COMPLETED` e o ZIP
`results/<job-id>/frames.zip`. No profile `local`, o frontend recebe
`https://shorturl.at/JpxZS`. Os UUIDs podem ser controlados por
`APP_VIDEO_DOWNLOAD_FIXTURE_VIDEO_ID` e `APP_VIDEO_DOWNLOAD_FIXTURE_JOB_ID`;
execuções repetidas são idempotentes.

Para simular outro proprietário, informe o e-mail e o UUID do vídeo nas mesmas
variáveis. Se já existir um job visível para esse vídeo, a fixture reutiliza o
`resultKey` persistido e materializa o ZIP nesse caminho; assim, o job não precisa
ser recriado.

### Reprocessar um evento esgotado

Após corrigir a causa indicada por `eventId` e `errorCode` no log, um operador
pode recolocar o registro `FAILED` na fila com SQL:

```sql
UPDATE outbox_events
SET status = 'PENDING', attempts = 0, next_attempt_at = now(), last_error_code = NULL
WHERE id = '<event-id>' AND status = 'FAILED';
```

O update deve afetar uma linha. Como a falha pode ter ocorrido após a confirmação
do broker, o reprocessamento pode produzir duplicata; consumidores devem
continuar idempotentes por `eventId`.

O comando Maven usa o perfil `local`, com credenciais de desenvolvimento e par
RSA efêmero. Os JWTs emitidos deixam de valer após reiniciar a API. Não use esse
perfil em ambiente compartilhado ou de produção.

Verifique a aplicação em `http://localhost:8080/actuator/health`. O contrato
HTTP está em `http://localhost:8080/openapi.yaml` e o Swagger UI local em
`http://localhost:8080/swagger-ui.html`. Para parar as dependências sem apagar
os volumes:

```bash
docker compose stop postgres rabbitmq minio
```

## Banco de dados

PostgreSQL é a fonte de verdade dos metadados. O Flyway aplica as migrations em
`src/main/resources/db/migration` e o Hibernate valida o schema, sem alterá-lo.
O DER, as relações, os índices e a estratégia de evolução estão em
[docs/video-api-database.md](docs/video-api-database.md).

## Verificação

```bash
./mvnw -pl services/video-api -am verify
```

Os testes de integração com PostgreSQL via Testcontainers exigem Docker em
execução. O módulo também possui gate de cobertura JaCoCo de 70%.
