---
name: video-api-job-lifecycle
description: Aplicar regras de transição, histórico e idempotência ao alterar o ciclo de vida de jobs em services/video-api. Não use para mudanças sem estados de job ou consumo de eventos relacionado.
---

# Ciclo de vida dos jobs

Use esta skill ao alterar estados de jobs, transições ou o consumo de eventos que atualiza seu ciclo de vida em `services/video-api`.

- Defina explicitamente as transições permitidas entre estados do contrato.
  Rejeite transições inválidas e trate eventos antigos sem regredir o estado.
- Toda mudança efetiva de status persiste seu histórico na mesma transação.
- Criação do job e seu evento na outbox pertencem à mesma transação.
- Eventos podem chegar repetidos ou fora de ordem. Deduplicação deve ser durável
  e segura sob concorrência; não basta um `Set` em memória.
- Aplicação do resultado e registro de evento tratado precisam ser atômicos.
  Confirme consumo somente após sucesso da transação.
- Não permita evento antigo sobrescrever resultado ou estado terminal.

Leia os [exemplos de comportamento](references/exemplos.md) quando precisar avaliar duplicação, ordenação ou concorrência.

Não faça `job.setStatus(event.status())` diretamente no listener. Encaminhe ao
caso de uso que valida transição e persiste efeitos atomicamente. Não compare
apenas timestamps para decidir precedência: respeite a máquina de estados e a
política de ordenação do contrato/especificação.

Quando uma transição ainda não estiver especificada, registre a questão antes de
inventar uma política. Teste transições permitidas, inválidas, duplicatas e ordem
invertida; consulte a [skill de persistência e mensageria](../video-api-persistence-messaging/SKILL.md).
