# Atributos de qualidade

## Objetivo

Os atributos abaixo transformam intenções arquiteturais em cenários verificáveis. Metas numéricas iniciais são propostas para orientar testes; devem ser confirmadas com o responsável pelo produto antes de virarem SLOs contratuais.

## Cenários e táticas

| Atributo | Cenário | Resposta esperada | Táticas | Evidência atual |
|---|---|---|---|---|
| Disponibilidade | RabbitMQ fica indisponível após criação do job | job permanece recuperável e é publicado depois | outbox transacional, polling | schema e publisher existentes |
| Confiabilidade | consumer cai após executar efeito, antes do ack | redelivery não duplica efeito final | `eventId`, unique constraints, keys determinísticas | parcial; inbox pendente em dois consumers |
| Escalabilidade | fila cresce durante pico | adicionar processors aumenta vazão sem escalar API | serviços stateless, work queue, S3 | desenho e containers separados |
| Performance | cliente cria job durante processamento pesado | API mantém latência independente do FFmpeg | processamento fora da API | separação implementada |
| Segurança | usuário tenta consultar job de terceiro | resposta não revela existência nem dados | JWT + ownership check | JWT existe; ownership pendente |
| Observabilidade | job fica parado | operador identifica etapa e correlação | métricas, logs com `jobId`, alertas | Actuator/Prometheus; tracing pendente |
| Recuperabilidade | mensagem vai para DLQ | operador corrige e reprocessa com segurança | DLQ, runbook, idempotência | filas existentes; runbook pendente |
| Manutenibilidade | contrato muda de forma incompatível | v1 e v2 convivem durante migração | AsyncAPI, versionamento no routing key | contrato v1 existente |

## Metas iniciais propostas

| Indicador | Meta de partida | Observação |
|---|---:|---|
| disponibilidade mensal da API | 99,5% | exclui manutenção acordada |
| p95 de criação/consulta de job | < 300 ms | sem incluir upload/download |
| tempo até início em carga normal | < 30 s | depende da profundidade da fila |
| jobs perdidos após resposta 201 | 0 | garantido por persistência + outbox |
| mensagens em DLQ sem alerta | 0 por mais de 5 min | requer integração de alertas |
| RPO do banco | ≤ 15 min | depende da estratégia de backup |
| RTO do serviço | ≤ 60 min | objetivo inicial de recuperação |

## Confiabilidade

O sistema prefere entrega pelo menos uma vez a perda silenciosa. Isso desloca complexidade para idempotência, transições monotônicas e efeitos externos. O banco protege a criação do job, mas a coordenação com broker, S3 e SMTP permanece eventualmente consistente.

Testes necessários: queda do broker antes/depois do publish, redelivery após efeito, ZIP já existente, evento fora de ordem, indisponibilidade SMTP e replay de DLQ.

## Escalabilidade e capacidade

API e workers devem escalar independentemente. Para o processor, CPU, memória, disco temporário e banda são limites mais relevantes que número de threads. A política de autoscaling deve considerar fila pronta, idade da mensagem mais antiga e duração média, com teto que respeite storage e broker.

## Segurança e privacidade

Princípios: menor privilégio, secrets fora do código, TLS em trânsito, criptografia em repouso, autorização por proprietário, conteúdo binário fora de banco/broker e retenção mínima. `reason` de falha e logs devem ser sanitizados. Dependências e imagens precisam de scan e SBOM no pipeline futuro.

## Observabilidade

### Métricas mínimas

- HTTP: taxa, erro, duração e saturação;
- jobs: total por estado e tempo em cada estado;
- outbox: backlog, idade máxima, tentativas e falhas;
- RabbitMQ: ready, unacked, redelivery e DLQ;
- processor: duração por etapa, bytes, frames, timeout e disco;
- notification: entrega, latência, duplicata, retry e erro SMTP.

### Correlação

Propagar `eventId`, `jobId`, `correlationId` e `traceparent`. Logs devem ser JSON em produção e nunca registrar JWT, credencial S3 ou payload sensível.

## Validação contínua

CI deve cobrir unitários, integração com Testcontainers, contratos, migrations e Compose. Antes de produção: teste de carga, fault injection básica, restore de backup, scan de imagem, smoke test e exercício documentado de DLQ.
