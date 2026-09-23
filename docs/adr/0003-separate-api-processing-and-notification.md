# ADR 0003 — Separar API, processamento e notificação

- **Status:** Aceito
- **Data:** 2026-08-30

## Contexto

Requisições HTTP são curtas e sensíveis a latência; processamento de vídeo é intensivo em CPU, disco e tempo; envio de
e-mail depende de um sistema externo com falhas próprias. Colocar esses comportamentos no mesmo processo faria uma carga
ou falha afetar todo o produto.

## Alternativas consideradas

1. **Três aplicações independentes:** isolamento e escala por workload, com maior custo operacional.
2. **API mais processor, notificação externa:** reduz um serviço, mas mantém FFmpeg competindo com HTTP.
3. **Monólito único:** deploy simples, porém alto acoplamento de recursos e menor resiliência.
4. **Funções serverless para processamento:** escala elástica, mas limites de duração, disco e tamanho podem conflitar
   com vídeos grandes.

## Decisão

Criar `video-api`, `video-processor` e `notification-worker` como aplicações Spring Boot independentes. Cada uma terá
build, configuração, container, health check e escala próprios. A comunicação entre serviços ocorrerá por contratos
AMQP; nenhum serviço acessará tabelas de outro bounded context.

## Consequências positivas

- FFmpeg pode escalar por profundidade da fila sem aumentar réplicas da API;
- indisponibilidade SMTP não bloqueia processamento nem HTTP;
- deploys e limites de CPU/memória podem ser ajustados por perfil.

## Consequências negativas e riscos

- maior quantidade de artefatos, logs, dashboards e pipelines;
- consistência eventual e necessidade de correlação distribuída;
- testes end-to-end mais complexos.

## Mitigações e revisão

Padronizar métricas, IDs de correlação, contratos e templates de pipeline. Rever a divisão se o custo operacional
superar os benefícios medidos; a união de serviços exigiria novo ADR e preservação dos limites de domínio.
