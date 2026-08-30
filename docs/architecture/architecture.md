# Arquitetura da solução

## Objetivo e escopo

Esta arquitetura separa a interação HTTP, o processamento pesado de mídia e a notificação de falhas. O objetivo é absorver picos, permitir escala independente e impedir que operações longas de FFmpeg consumam recursos da API. O desenho representa a fundação atual; lacunas conhecidas aparecem no final do documento.

## Diagrama de contexto

```mermaid
flowchart TB
    USER[Usuário / cliente HTTP]
    SYSTEM[FIAP X Video Processing Platform]
    IDP[Provedor de identidade<br/>futuro]
    SMTP[Servidor SMTP]
    USER -->|cria e consulta jobs<br/>baixa resultados| SYSTEM
    IDP -.->|emite JWT<br/>não implementado nesta fundação| USER
    SYSTEM -->|notifica falha terminal| SMTP
```

## Diagrama de containers

```mermaid
flowchart LR
    CLIENT[Cliente]
    subgraph PLATFORM[FIAP X]
      API[video-api<br/>Spring Boot]
      PROCESSOR[video-processor<br/>Spring Boot + FFmpeg]
      NOTIFICATION[notification-worker<br/>Spring Boot]
      RABBIT[(RabbitMQ<br/>video.events)]
      API_DB[(PostgreSQL<br/>jobs + outbox)]
      NOTIFICATION_DB[(PostgreSQL<br/>notification deliveries)]
      STORAGE[(MinIO / S3<br/>vídeos + ZIPs)]
    end
    MAIL[SMTP / MailHog]
    CLIENT -->|HTTPS + JWT| API
    CLIENT -.->|upload/download futuro| STORAGE
    API -->|JPA + Flyway| API_DB
    API -->|publica outbox| RABBIT
    RABBIT -->|requested.v1| PROCESSOR
    PROCESSOR -->|GET vídeo / PUT ZIP| STORAGE
    PROCESSOR -->|started/completed/failed| RABBIT
    RABBIT -->|resultados| API
    RABBIT -->|falhas| NOTIFICATION
    NOTIFICATION --> NOTIFICATION_DB
    NOTIFICATION --> MAIL
```

## Responsabilidades e propriedade

| Componente | Responsabilidades | Dados próprios | Não deve fazer |
|---|---|---|---|
| `video-api` | validar JWT, criar/consultar jobs, registrar outbox, aplicar resultados | jobs, histórico, outbox | processar mídia ou acessar banco de notificações |
| `video-processor` | validar vídeo, extrair frames, gerar ZIP, publicar resultados | arquivos temporários efêmeros | atualizar tabelas da API ou transportar binários no broker |
| `notification-worker` | consumir falhas terminais, enviar e-mail, auditar entrega | entregas de notificação | consultar jobs/usuários diretamente no banco da API |
| RabbitMQ | filas de trabalho, fan-out lógico, retry e DLQ | mensagens pequenas e temporárias | armazenar vídeos ou ZIPs |
| MinIO/S3 | objetos de entrada e saída | vídeo e ZIP | atuar como fonte de verdade do estado do job |

## Fluxo principal

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant A as video-api
    participant D as PostgreSQL API
    participant R as RabbitMQ
    participant P as video-processor
    participant S as MinIO/S3
    C->>A: POST /v1/jobs {sourceKey}
    A->>D: BEGIN
    A->>D: INSERT job PENDING
    A->>D: INSERT outbox requested.v1
    A->>D: COMMIT
    A-->>C: 201 Job PENDING
    loop publicação da outbox
      A->>D: busca eventos não publicados
      A->>R: video.job.requested.v1
      A->>D: marca publishedAt
    end
    R->>P: entrega job requested
    P->>R: video.job.started.v1
    R->>A: started
    A->>D: status PROCESSING
    P->>S: baixa sourceKey
    P->>P: FFprobe + FFmpeg + ZIP
    P->>S: envia resultKey
    P->>R: video.job.completed.v1
    R->>A: completed
    A->>D: status COMPLETED + resultKey
    C->>A: GET /v1/jobs/{id}
    A-->>C: COMPLETED + resultKey
```

## Fluxo de falha, retry e DLQ

```mermaid
flowchart TD
    MSG[Mensagem requested.v1] --> PROC[Executar processamento]
    PROC --> OK{Sucesso?}
    OK -->|sim| DONE[Publicar completed.v1]
    OK -->|não| FAIL[Publicar failed.v1]
    FAIL --> RETRY{Tentativas menores que 4?}
    RETRY -->|sim| BACKOFF[Backoff 2s, 4s, 8s...] --> PROC
    RETRY -->|não| DLQ[video.processing.dlq.v1]
    FAIL --> API[API registra FAILED]
    FAIL --> NOTIF[Worker tenta notificar]
    NOTIF --> MAILOK{E-mail enviado?}
    MAILOK -->|sim| AUDIT[Persistir eventId da entrega]
    MAILOK -->|não, após retries| NDLQ[notification failure DLQ]
```

Publicar `failed.v1` em cada tentativa pode produzir notificações prematuras na implementação inicial. A evolução recomendada é classificar erros e publicar falha terminal somente depois de esgotar o retry, preferencialmente por um recoverer associado à DLQ.

## Estados do job

```mermaid
stateDiagram-v2
    [*] --> PENDING: job + outbox persistidos
    PENDING --> PROCESSING: started.v1
    PROCESSING --> COMPLETED: completed.v1
    PROCESSING --> FAILED: failed.v1 terminal
    PENDING --> FAILED: falha antes do evento started
    COMPLETED --> [*]
    FAILED --> [*]
```

Transições duplicadas precisam ser seguras, pois a entrega é pelo menos uma vez. Regressões de estado e eventos fora de ordem devem ser rejeitados quando a máquina de estados for endurecida.

## Topologia RabbitMQ

```mermaid
flowchart LR
    EX((video.events<br/>topic exchange))
    EX -->|video.job.requested.v1| PQ[video.processing.v1]
    PQ -.->|retries esgotados| PDLQ[video.processing.dlq.v1]
    EX -->|started/completed/failed| AQ[video.api.results.v1]
    AQ -.->|retries esgotados| ADLQ[video.api.results.dlq.v1]
    EX -->|video.job.failed.v1| NQ[video.notifications.failure.v1]
    NQ -.->|retries esgotados| NDLQ[video.notifications.failure.dlq.v1]
```

## Diagrama de implantação local

```mermaid
flowchart TB
    subgraph DOCKER[Docker Compose network]
      API[video-api :8080]
      PROC[video-processor :8081]
      NOTIF[notification-worker :8082]
      PG[(PostgreSQL :5432)]
      MQ[(RabbitMQ :5672 / UI :15672)]
      MINIO[(MinIO :9000 / UI :9001)]
      MAIL[(MailHog :1025 / UI :8025)]
      INIT[minio-init]
      INIT -->|cria bucket videos| MINIO
      API --> PG
      API --> MQ
      PROC --> MQ
      PROC --> MINIO
      NOTIF --> PG
      NOTIF --> MQ
      NOTIF --> MAIL
    end
```

Em produção, cada aplicação deve ser uma unidade de deploy independente. Banco, broker, storage e SMTP devem ser serviços gerenciados ou operados com políticas próprias de backup, disponibilidade, TLS e credenciais.

## Consistência e garantias

- Criar o job e registrar a intenção de publicação é atômico dentro do banco da API.
- Publicar no RabbitMQ e marcar a outbox não formam uma única transação distribuída; duplicatas são possíveis.
- Consumers recebem mensagens pelo menos uma vez e devem deduplicar por `eventId`.
- A tabela do notification worker já possui `event_id` único. A deduplicação persistente do processor e do result listener permanece no roadmap.
- Object storage e banco têm consistência eventual: o job só deve virar `COMPLETED` depois que o upload do ZIP finalizar.

## Segurança

O limite HTTP exige bearer JWT, mas a emissão e a gestão de identidades não fazem parte desta fundação. Object keys não devem ser expostas como autorização de acesso; URLs pré-assinadas curtas e validação de propriedade devem mediar upload e download. Produção também requer TLS, chaves assimétricas, rotação de segredos, usuário de banco por serviço e políticas mínimas de bucket e RabbitMQ.

## Observabilidade e operação

Actuator oferece health/probes e Micrometer expõe Prometheus. Para operação real ainda são necessários logs estruturados com `jobId`/`eventId`, tracing entre HTTP e AMQP, métricas de lag, idade da outbox, duração de FFmpeg, taxa de falha, profundidade das DLQs, dashboards e alertas. Consulte [quality-attributes.md](quality-attributes.md).

## Lacunas conhecidas

1. Upload/download por URL pré-assinada ainda não foi implementado.
2. O projeto valida, mas não emite JWT; autorização de propriedade do job precisa ser adicionada.
3. Processor e API result listener ainda precisam de inbox/deduplicação persistente.
4. O publisher da outbox precisa de claim concorrente, publisher confirms e recuperação explícita.
5. Testes unitários, Testcontainers e end-to-end ainda precisam ser implementados.
6. Eventos de falha devem carregar o destinatário ou uma referência resolvível sem acesso ao banco da API.
7. A política de retenção e limpeza de objetos temporários ainda deve ser definida.
