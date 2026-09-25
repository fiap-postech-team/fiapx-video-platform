# Observabilidade local do FIAP X

Esta configuração cobre o `video-api`. Actuator e Micrometer fornecem métricas HTTP, JVM, processo, HikariCP e executores quando aplicáveis; as métricas de domínio abaixo completam essa instrumentação.

## Logs e correlação

O `video-api` escreve logs JSON no formato Logstash. O resumo HTTP é emitido para operações que alteram estado e para qualquer resposta de erro; leituras bem-sucedidas, como `GET /v1/me`, não geram essa linha. O registro inclui método, rota sem identificadores, status, resultado e duração. Actuator e Swagger também são omitidos para evitar ruído.

O filtro aceita `X-Correlation-Id` como UUID. Se o header não vier ou não for um UUID válido, a API gera um UUID; em ambos os casos, devolve o valor no header da resposta. O valor aparece no MDC/log como `correlationId`, é propagado no payload e no header AMQP da outbox, e os consumidores de resultados adicionam `correlationId` e `jobId` ao contexto durante o processamento.

Logs operacionais usam `operation`, `result`, `durationMs`, `errorCode` e `errorType`, quando aplicável. Não registram Authorization, JWT, senhas, refresh tokens, credenciais, URLs assinadas, payloads, `sourceKey` ou mensagens de exceção não sanitizadas. IDs podem aparecer nos logs para diagnóstico, mas nunca são labels de métricas.

## Métricas

O Micrometer instrumenta automaticamente requisições HTTP com `http_server_requests_seconds_count`, `_sum` e `_max`, com dimensões de baixa cardinalidade como método, rota e status. Também publica métricas padrão de JVM, processo, datasource e executor.

Métricas de domínio do `video-api`:

| Métrica Prometheus | Significado |
|---|---|
| `fiapx_uploads_created_events_total` | Uploads persistidos |
| `fiapx_uploads_confirmed_total` | Primeiras confirmações de upload |
| `fiapx_uploads_expired_total` | Transições para expirado |
| `fiapx_jobs_created_events_total` | Jobs criados, sem contar retornos idempotentes |
| `fiapx_jobs_completed_total` | Jobs que transitaram para concluído |
| `fiapx_jobs_failed_total` | Jobs que transitaram para falho |
| `fiapx_jobs_duration_seconds_count`, `_sum`, `_max` | Duração entre criação e primeiro resultado terminal aplicado |
| `fiapx_event_publication_failures_total` | Tentativas de publicação que falharam; label limitada `error_code` |
| `fiapx_event_consumption_failures_total` | Falhas/retries de consumo; label limitada `failure_stage` |

Também são mantidas as métricas existentes da outbox e do inbox, incluindo `outbox_backlog`, `outbox_oldest_event_age_seconds`, `outbox_failed_records`, tentativas e publicações, confirms negativos, claims expirados, duração de batch e contadores de resultados recebidos, processados, rejeitados e retries.

Nenhuma métrica usa `userId`, `jobId`, `eventId` ou `correlationId` como label. As labels customizadas de falha vêm de conjuntos fixos de códigos/estágios.

## Executar e consultar

Na raiz do repositório, inicie o ambiente local:

```bash
cp .env.example .env
docker compose up --build
```

O Prometheus coleta `video-api:8080/actuator/prometheus` a cada 15 segundos. Consulte o endpoint diretamente em [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) e abra [http://localhost:9090](http://localhost:9090) para consultar as séries.

Se a porta host `8080` já estiver ocupada, publique a API em outra porta sem alterar o target interno do Prometheus:

```bash
VIDEO_API_HTTP_PORT=18080 docker compose up --build
```

Nesse caso, o endpoint da API fica em `http://localhost:18080/actuator/prometheus`.

Para verificar a coleta, abra **Status → Targets** no Prometheus e confirme que o target `video-api` está `UP`. Também é possível executar `up{job="video-api"}` no campo de consulta: o resultado `1` indica coleta ativa. `0` ou ausência de série indica que o endpoint ou o target não está acessível.

Exemplos de PromQL:

```promql
# Requisições por segundo, todas as rotas
sum(rate(http_server_requests_seconds_count[5m]))

# Erros HTTP 5xx por segundo
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))

# Latência média HTTP em segundos
sum(rate(http_server_requests_seconds_sum[5m]))
  / sum(rate(http_server_requests_seconds_count[5m]))

# Uploads criados na última hora
sum(increase(fiapx_uploads_created_events_total[1h]))

# Jobs concluídos na última hora
sum(increase(fiapx_jobs_completed_total[1h]))

# Jobs criados na última hora (sem retornos idempotentes)
sum(increase(fiapx_jobs_created_events_total[1h]))

# Duração média dos jobs concluídos/falhos observados
sum(rate(fiapx_jobs_duration_seconds_sum[5m]))
  / sum(rate(fiapx_jobs_duration_seconds_count[5m]))

# Falhas de publicação por código sanitizado
sum by (error_code) (increase(fiapx_event_publication_failures_total[15m]))

# Falhas de consumo por etapa
sum by (failure_stage) (increase(fiapx_event_consumption_failures_total[15m]))

# Backlog pendente ou em processamento da outbox
outbox_backlog

# Estado do scrape do video-api (1 = UP)
up{job="video-api"}
```
