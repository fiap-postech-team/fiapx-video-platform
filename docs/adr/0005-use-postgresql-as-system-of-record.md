# ADR 0005 — Usar PostgreSQL como fonte de verdade

- **Status:** Aceito
- **Data:** 2026-08-30

## Contexto

Usuários, jobs, transições, outbox, tentativas e auditoria exigem consistência, constraints, consultas operacionais e
migrations reproduzíveis. Object storage e RabbitMQ não oferecem o mesmo modelo transacional para esses dados.

## Alternativas consideradas

1. **PostgreSQL:** transações ACID, índices e ecossistema maduro, com necessidade de dimensionamento e manutenção.
2. **Banco documental:** flexibilidade de schema, mas menor ganho para dados relacionais e transições controladas.
3. **RabbitMQ como estado:** inadequado para consulta e retenção de negócio.
4. **Um único schema compartilhado:** simples localmente, mas cria acoplamento e permite acessos cruzados indevidos.

## Decisão

PostgreSQL será a fonte de verdade. A API será proprietária de usuários, jobs, histórico e outbox. O notification worker
manterá armazenamento lógico separado para idempotência e auditoria. O processor não atualizará tabelas da API.
Migrations serão aplicadas com Flyway por aplicação.

## Consequências positivas

- invariantes protegidos por transações e constraints;
- suporte natural ao padrão outbox;
- auditoria e diagnóstico com ferramentas difundidas.

## Consequências negativas e riscos

- mais de um banco/schema aumenta operação, backup e observabilidade;
- consultas cruzadas entre contextos deixam de ser triviais;
- migrations incompatíveis podem impedir startup.

## Mitigações e revisão

Credenciais distintas por serviço, migrations backward-compatible, backups testados e métricas de conexão/latência.
Integrações entre contextos devem usar eventos ou APIs. Rever apenas diante de requisitos mensuráveis que o PostgreSQL
não atenda.
