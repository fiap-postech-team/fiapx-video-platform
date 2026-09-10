# Exemplos

| Cenário | Comportamento exigido |
|---|---|
| Mesma conclusão entregue duas vezes | Uma mudança de estado e um histórico. |
| Início chega após conclusão | Preservar estado terminal e resultado. |
| Falha ao gravar histórico | Rollback da mudança de estado. |
| Duas entregas concorrentes do mesmo evento | Garantia no banco impede dois efeitos. |
