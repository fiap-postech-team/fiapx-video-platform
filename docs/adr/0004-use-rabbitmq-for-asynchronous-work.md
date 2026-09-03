# ADR 0004 — Usar RabbitMQ para processamento assíncrono

- **Status:** Aceito
- **Data:** 2026-08-30

## Contexto

Processar vídeo pode levar muito mais tempo que o timeout aceitável de uma chamada HTTP. O sistema precisa absorver
picos, controlar concorrência, reentregar falhas transitórias e distribuir eventos a consumidores distintos.

## Forças de decisão

- filas de trabalho com acknowledgment;
- routing por topic exchange;
- retry limitado e DLQ;
- operação local simples via Docker Compose.

## Alternativas consideradas

1. **RabbitMQ:** adequado para work queues e roteamento, mas exige cuidado com confirmação, prefetch e DLQs.
2. **Apache Kafka:** excelente para replay e alto throughput, porém operacionalmente mais pesado e menos natural para
   distribuição de tarefas individuais.
3. **Polling no PostgreSQL:** reduz infraestrutura, mas aumenta contenção e mistura agendamento com a fonte de verdade.
4. **Processamento síncrono:** simples, mas viola requisitos de latência, resiliência e escala.

## Decisão

Usar um topic exchange durável, filas duráveis por consumidor e routing keys versionados. Mensagens conterão somente
IDs, object keys e metadados pequenos. Consumers usarão acknowledgment após sucesso, retries limitados e DLQ após
esgotamento.

## Consequências positivas

- desacoplamento temporal entre API e processor;
- backpressure visível pela profundidade da fila;
- um evento de falha pode alimentar API e notificações sem chamada direta.

## Consequências negativas e riscos

- duplicatas e possível reordenação;
- broker vira dependência operacional crítica;
- configurações incorretas podem criar loops de redelivery ou mensagens órfãs.

## Mitigações e revisão

Usar idempotência por `eventId`, publisher confirms, métricas de fila, alarmes de DLQ e runbook de replay. Rever se
retenção longa, replay em larga escala ou throughput sustentado indicarem necessidade de log distribuído como Kafka.
