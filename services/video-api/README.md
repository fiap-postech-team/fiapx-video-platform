# video-api

API HTTP da plataforma FIAP X, desenvolvida em Java 21 e Spring Boot. É
responsável por cadastro e autenticação de usuários, upload direto de vídeos
para S3/MinIO, criação e consulta de jobs de processamento.

## Arquitetura

O serviço é organizado por capacidades (`identity`, `videos`, `jobs`, `outbox` e
`inbox`). PostgreSQL mantém identidades, metadados dos vídeos, jobs e eventos;
os arquivos ficam no object storage. O cliente envia o vídeo diretamente ao
S3/MinIO por URL pré-assinada e confirma o upload antes de criar um job.

Na criação do job, o estado `PENDING`, o histórico inicial e a intenção de
publicar `video.job.requested.v1` são persistidos na mesma transação. O schema e
as operações de outbox/inbox existem, mas o publisher e o listener RabbitMQ
ainda não estão integrados. O processamento de mídia pertence ao
`video-processor`, não à API.

Contratos: [OpenAPI](../../contracts/openapi.yaml) e
[AsyncAPI](../../contracts/asyncapi.yaml).

## Como executar localmente

Pré-requisitos: JDK 21, Docker e portas `5432`, `8080` e `9000` livres. Na raiz
do monorepo:

```bash
docker compose up -d postgres minio minio-init
./mvnw -pl services/video-api -am spring-boot:run
```

O comando Maven usa o perfil `local`, com credenciais de desenvolvimento e par
RSA efêmero. Os JWTs emitidos deixam de valer após reiniciar a API. Não use esse
perfil em ambiente compartilhado ou de produção.

Verifique a aplicação em `http://localhost:8080/actuator/health`. O contrato
HTTP está em `http://localhost:8080/openapi.yaml` e o Swagger UI local em
`http://localhost:8080/swagger-ui.html`. Para parar as dependências sem apagar
os volumes:

```bash
docker compose stop postgres minio
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
