# ADR 0012 — Evoluir o schema do video-api com migrations forward-only

- **Status:** Aceito
- **Data:** 2026-09-17

## Contexto

O PostgreSQL é a fonte de verdade do `video-api`, mas a migration inicial só
modelava jobs, histórico e uma outbox textual. A autenticação local, o upload
confirmado, a autorização por proprietário, a idempotência e a operação segura
da outbox exigem relações e invariantes adicionais. A migration `V1` já é um
contrato publicado e não pode ser reescrita.

## Forças de decisão

- uma instância antiga e uma nova precisam poder coexistir no rollout;
- referências e eventos duplicados devem ser bloqueados pelo banco;
- entidades JPA não podem vazar para o domínio;
- o Hibernate deve apenas validar o schema, nunca gerá-lo.

## Alternativas consideradas

1. Alterar `V1`: simples localmente, mas invalida ambientes já provisionados.
2. Recriar todo o schema: perde dados e exige indisponibilidade.
3. Expandir em uma migration nova, manter campos legados e contratar depois:
   preserva compatibilidade e permite backfill seguro.

## Decisão

Usaremos migrations Flyway forward-only. A `V3` cria `users`, credenciais,
papéis, vídeos, inbox e idempotência; expande jobs, histórico, sessões e outbox
com checks, FKs e índices. `source_key`, `result_key` e o payload textual da
outbox permanecem durante a transição. As portas da aplicação isolam JPA e
delimitam as transações de job/histórico/outbox, inbox/resultado, refresh e
claim da outbox.

## Consequências

- constraints e índices passam a proteger ownership, idempotência e polling;
- o histórico permanece append-only e a versão do job detecta atualização
  concorrente;
- há dados legados temporários e uma futura migration de contract/limpeza;
- testes em PostgreSQL real são obrigatórios para validar a evolução.

## Critérios de revisão

Revisar quando os consumidores não dependerem mais de campos legados, quando a
retenção de inbox/outbox estiver definida ou quando volume justificar
particionamento.
