# Ciclo de vida dos jobs

- Defina explicitamente as transições permitidas entre estados do contrato.
  Rejeite transições inválidas e trate eventos antigos sem regredir o estado.
- Toda mudança efetiva de status persiste seu histórico na mesma transação.
- Criação do job e seu evento na outbox pertencem à mesma transação.
- Eventos podem chegar repetidos ou fora de ordem. Deduplicação deve ser durável
  e segura sob concorrência; não basta um `Set` em memória.
- Aplicação do resultado e registro de evento tratado precisam ser atômicos.
  Confirme consumo somente após sucesso da transação.
- Não permita evento antigo sobrescrever resultado ou estado terminal.

## Exemplos

| Cenário | Comportamento exigido |
|---|---|
| Mesma conclusão entregue duas vezes | Uma mudança de estado e um histórico. |
| Início chega após conclusão | Preservar estado terminal e resultado. |
| Falha ao gravar histórico | Rollback da mudança de estado. |
| Duas entregas concorrentes do mesmo evento | Garantia no banco impede dois efeitos. |

Não faça `job.setStatus(event.status())` diretamente no listener. Encaminhe ao
caso de uso que valida transição e persiste efeitos atomicamente. Não compare
apenas timestamps para decidir precedência: respeite a máquina de estados e a
política de ordenação do contrato/especificação.

Quando uma transição ainda não estiver especificada, registre a questão antes de
inventar uma política. Teste transições permitidas, inválidas, duplicatas e ordem
invertida; consulte [persistência e mensageria](persistence-messaging.md).
