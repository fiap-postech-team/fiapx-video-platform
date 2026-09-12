# Architecture Decision Records

ADRs registram decisões arquiteturais relevantes, o contexto disponível no momento, alternativas consideradas e
consequências aceitas. Eles explicam por que o sistema tomou determinado rumo; não substituem a documentação operacional
nem os contratos.

## Estados

- **Proposto:** em discussão, ainda não orienta implementação.
- **Aceito:** decisão vigente.
- **Substituído:** preservado para histórico e apontando para o ADR sucessor.
- **Rejeitado:** avaliado e não adotado.

Para alterar uma decisão aceita, crie um novo ADR e marque o anterior como substituído. ADRs são imutáveis após aceitos,
exceto por correções editoriais e links.

## Índice

| ADR                                                                 | Decisão                           | Status |
|---------------------------------------------------------------------|-----------------------------------|--------|
| [0001](0001-use-java-21-and-spring-boot.md)                         | Java 21 e Spring Boot 3           | Aceito |
| [0002](0002-use-a-maven-multi-module-monorepo.md)                   | Monorepo Maven multi-module       | Aceito |
| [0003](0003-separate-api-processing-and-notification.md)            | Separação dos três serviços       | Aceito |
| [0004](0004-use-rabbitmq-for-asynchronous-work.md)                  | RabbitMQ para trabalho assíncrono | Aceito |
| [0005](0005-use-postgresql-as-system-of-record.md)                  | PostgreSQL como fonte de verdade  | Aceito |
| [0006](0006-use-s3-compatible-object-storage.md)                    | Object storage compatível com S3  | Aceito |
| [0007](0007-use-transactional-outbox.md)                            | Outbox transacional               | Aceito |
| [0008](0008-use-at-least-once-delivery-and-idempotent-consumers.md) | Entrega pelo menos uma vez        | Aceito |
| [0009](0009-defer-redis-until-needed.md)                            | Adiar Redis                       | Aceito |

## Template

Novos registros devem conter: **Status**, **Data**, **Contexto**, **Forças de decisão**, **Alternativas consideradas**,
**Decisão**, **Consequências positivas**, **Consequências negativas e riscos**, **Mitigações** e **Critérios de revisão
**.
