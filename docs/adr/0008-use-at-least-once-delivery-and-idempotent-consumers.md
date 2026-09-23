# ADR 0008 — Aceitar entrega pelo menos uma vez e exigir idempotência

- **Status:** Aceito
- **Data:** 2026-08-30

## Contexto

Falhas podem ocorrer depois de um consumer concluir o efeito e antes do acknowledgment, ou depois de o publisher enviar
e antes de marcar a outbox. “Exactly once” entre banco, broker, storage e SMTP não é uma garantia realista sem
coordenação cara e restritiva.

## Alternativas consideradas

1. **At-least-once + idempotência:** não perde trabalho confirmado, mas exige deduplicação e efeitos repetíveis.
2. **At-most-once:** evita repetição, porém pode perder jobs em falhas transitórias.
3. **Exactly-once declarado pela aplicação:** aparência simples, mas não cobre todos os sistemas e cria falsa segurança.

## Decisão

Cada evento recebe `eventId` imutável. Consumers devem persistir uma inbox ou chave de negócio única na mesma transação
de seu efeito local. Operações de storage devem usar keys determinísticas. Retry será exponencial, limitado e
diferenciará falhas transitórias de terminais; ao esgotar, a mensagem seguirá para DLQ.

## Consequências positivas

- recuperação segura após quedas e timeouts;
- operação explícita de mensagens problemáticas;
- modelo alinhado às garantias do RabbitMQ e da outbox.

## Consequências negativas e riscos

- tabelas de deduplicação precisam de retenção e índices;
- side effects externos, como SMTP, não são atomicamente deduplicáveis com o banco;
- eventos fora de ordem ainda exigem regras de transição.

## Mitigações e revisão

Usar unique constraint por `eventId`, state machine monotônica, `jobId` nos logs e runbook de DLQ. No e-mail, persistir
intenção antes do envio e usar chave idempotente se o provedor suportar. A fundação atual implementa unicidade no
worker; processor e API ainda precisam completar essa decisão.
