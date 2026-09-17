# Catálogo de eventos

## Convenções

Eventos usam JSON UTF-8 e routing keys no formato `<domínio>.<agregado>.<fato>.v<major>`. O nome descreve um fato
passado; a versão major muda somente quando houver incompatibilidade. Todo evento possui `eventId` para deduplicação e
`jobId` para correlação.

O contrato executável está em [`../../contracts/asyncapi.yaml`](../../contracts/asyncapi.yaml). Este documento explica
semântica, ownership e operação.

## Catálogo

| Routing key              | Produtor               | Consumidores                       | Quando ocorre              | Campos específicos                                      |
|--------------------------|------------------------|------------------------------------|----------------------------|---------------------------------------------------------|
| `video.job.requested.v1` | `video-api` via outbox | `video-processor`                  | job, histórico e outbox confirmados | `userId`, `videoId` opcional, `sourceKey`              |
| `video.job.started.v1`   | `video-processor`      | `video-api`                        | processor inicia trabalho  | `type=PROCESSING`                                       |
| `video.job.completed.v1` | `video-processor`      | `video-api`                        | ZIP armazenado com sucesso | `type=COMPLETED`, `resultKey`                           |
| `video.job.failed.v1`    | `video-processor`      | `video-api`, `notification-worker` | falha declarada terminal   | `type=FAILED`, `terminal`, `reason`, `recipient` futuro |

## Envelope esperado

```json
{
  "eventId": "bbf73e16-ab32-441b-9020-adf2fc6b4425",
  "jobId": "a96aa430-b106-4dcc-b340-03adfc1cb51b",
  "type": "COMPLETED",
  "occurredAt": "2026-08-30T20:00:00Z",
  "correlationId": "a96aa430-b106-4dcc-b340-03adfc1cb51b",
  "resultKey": "results/a96aa430-b106-4dcc-b340-03adfc1cb51b/frames.zip"
}
```

`occurredAt` e `correlationId` são persistidos no envelope da outbox. O payload
legado em texto permanece temporariamente para compatibilidade; novas consultas
operacionais usam o envelope JSONB versionado.

## Semântica por evento

### `video.job.requested.v1`

Comando durável para iniciar processamento. O processor pode recebê-lo mais de uma vez. `sourceKey` identifica um objeto
já existente e `videoId`, quando presente, identifica os metadados confirmados. Nenhum deles deve conter URL com
credencial. Retenção precisa cobrir a indisponibilidade máxima aceitável do processor.

### `video.job.started.v1`

Indica que uma tentativa começou, não que concluirá. Duplicatas não devem regredir estado. Se tentativas forem modeladas
no futuro, o evento deverá carregar `attempt` ou `processingId`.

### `video.job.completed.v1`

Só pode ser publicado após upload integral do ZIP. `resultKey` é referência, não prova de autorização. A API deve
garantir que o solicitante possui o job antes de gerar acesso ao objeto.

### `video.job.failed.v1`

Deve representar falha terminal. Erros transitórios não deveriam notificar o usuário a cada tentativa. `reason` precisa
ser seguro para exibição e não conter stack trace, segredo, comando ou dados pessoais.

## Compatibilidade

- Campos novos e opcionais podem ser adicionados em `v1`.
- Campos obrigatórios não podem ser removidos, renomeados ou mudar de tipo em `v1`.
- Mudança incompatível cria routing key `.v2` e período de dual publish/consume.
- Consumers devem ignorar campos desconhecidos.
- Schemas devem ser validados no CI quando o validador AsyncAPI for incorporado.

## Retry e DLQ

| Fila                             | DLQ                                  | Owner operacional     |
|----------------------------------|--------------------------------------|-----------------------|
| `video.processing.v1`            | `video.processing.dlq.v1`            | time de processamento |
| `video.api.results.v1`           | `video.api.results.dlq.v1`           | time da API           |
| `video.notifications.failure.v1` | `video.notifications.failure.dlq.v1` | time de notificações  |

Replay de DLQ exige corrigir a causa, verificar idempotência, registrar operador/motivo e acompanhar o resultado. Nunca
mova mensagens em massa sem limitar taxa e observar os consumidores.
