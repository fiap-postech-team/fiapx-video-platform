---
name: video-api-persistence-messaging
description: Aplicar regras de Flyway, JPA, outbox, publicação RabbitMQ e concorrência ao alterar persistência ou eventos em services/video-api. Não use para mudanças HTTP sem estado ou mensageria.
---

# Persistência e mensageria

Use esta skill ao alterar banco, migrations, outbox, publicação ou consumo de
eventos no Video API. Consulte `event-driven-messaging` para padrões de transporte
e `video-api-job-lifecycle` para transições de estado.

- Coloque fronteiras transacionais nos serviços de aplicação, não nos controllers.
- Use novas migrations Flyway forward-only. Nunca edite migration aplicada.
- Evite leituras ilimitadas e N+1: use paginação, lotes limitados e consultas
  adequadas ao volume esperado.
- Grave estado e outbox na mesma transação. Publicação direta no RabbitMQ durante
  criação do job não substitui a outbox.
- Publique somente registros pendentes. Marque publicado apenas após confirmação
  de sucesso do broker, considerando também mensagens não roteadas.
- Coordene publicadores concorrentes com claim/locking durável e recuperação de
  trabalho interrompido. Duplicação ainda é possível após envio e antes do registro
  local; consumidores devem ser idempotentes.
- Configure tamanho dos lotes, polling e retry. Limite retentativas, com backoff e
  tratamento de mensagens inválidas conforme a política de DLQ, sem loop infinito.
- Preserve compatibilidade e versão dos eventos no AsyncAPI. Inclua ID estável,
  timestamp de ocorrência, correlação, tipo e versão do schema.

Teste rollback job/outbox e concorrência no PostgreSQL real; valide confirms,
redelivery e falhas de roteamento no RabbitMQ real quando essas semânticas forem
relevantes. Mocks não comprovam garantias de integração.
