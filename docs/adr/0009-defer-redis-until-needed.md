# ADR 0009 — Adiar Redis até existir necessidade mensurável

- **Status:** Aceito
- **Data:** 2026-08-30

## Contexto

Redis é frequentemente introduzido para cache, locks, sessões ou rate limiting antes de existir um gargalo comprovado. O sistema já opera PostgreSQL, RabbitMQ e object storage; outro datastore aumenta custo, superfície de falha e conhecimento operacional.

## Alternativas consideradas

1. **Não introduzir Redis agora:** arquitetura menor e fonte de verdade clara, com menos opções de otimização imediata.
2. **Redis para cache de jobs:** reduz leituras, mas cria invalidação e pode devolver estado obsoleto num fluxo já eventual.
3. **Redis para locks:** útil em coordenação distribuída, porém PostgreSQL e RabbitMQ já oferecem mecanismos suficientes para a escala inicial.
4. **Redis para rate limiting:** adequado em múltiplas réplicas, mas não há requisito ou taxa medidos nesta etapa.

## Decisão

Não adicionar Redis à primeira versão. Consultas de job usam PostgreSQL; distribuição de trabalho usa RabbitMQ; objetos ficam no S3. Qualquer proposta futura deve definir problema, métrica atual, meta, modelo de falha, política de expiração e impacto de consistência.

## Consequências positivas

- menos infraestrutura, segredos, backups, patches e alertas;
- comportamento mais simples e fonte de verdade inequívoca;
- evita cache sem estratégia de invalidação.

## Consequências negativas e riscos

- rate limiting distribuído exigirá solução adicional quando houver múltiplas réplicas;
- leituras muito frequentes podem pressionar PostgreSQL;
- certas coordenações poderão demandar implementação mais cuidadosa no banco.

## Critérios de revisão

Reavaliar diante de evidência como saturação de leitura, latência fora do SLO, necessidade de rate limiting global ou lock com contenção não atendida. A adoção exigirá novo ADR, testes de indisponibilidade e estratégia para impedir que Redis se torne fonte de verdade acidental.
