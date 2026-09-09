# Persistência e mensageria

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

## Exemplos de revisão

| Não fazer | Fazer |
|---|---|
| Salvar job e depois chamar `rabbitTemplate.convertAndSend` | Salvar job/outbox atomicamente; publicador envia depois. |
| Marcar publicado antes de enviar | Aguardar confirmação do broker. |
| Gerar novo eventId por retry | Reutilizar o ID persistido na outbox. |
| Dois workers fazem `findAll` dos pendentes | Claim concorrente e lote limitado. |
| Alterar `V1__api_schema.sql` aplicada | Nova migration com próxima versão disponível. |
| Carregar todos os jobs para filtrar usuário em Java | Filtrar proprietário e limitar resultados no banco. |

Teste rollback job/outbox e concorrência no PostgreSQL real; valide confirms,
redelivery e falhas de roteamento no RabbitMQ real quando essas semânticas forem
relevantes. Mocks não comprovam garantias de integração.
