# Testes e cobertura

## Regra crítica e obrigatória

Todo comportamento novo ou alterado e toda correção devem ter testes automatizados.
Esta regra é crítica e não pode ser ignorada. Cubra sucesso, falhas relevantes e
limites. Documentação sem mudança de comportamento não exige testes artificiais.
Cada teste valida um conceito específico; múltiplas asserções são permitidas
quando descrevem o mesmo resultado.

## TDD e cobertura

- Escreva o teste primeiro e confirme falha pelo motivo correto (Red).
- Implemente o mínimo para passar (Green), refatore e execute a regressão afetada.
- Exija pelo menos **80% de linhas e 80% de branches** do código de produção do
  módulo, agregando testes unitários e de integração sem duplicar contagens.
- Priorize autorização, transições, atomicidade job/outbox, deduplicação e falhas
  do broker. Atingir o percentual não dispensa esses cenários críticos.
- Não exclua código de negócio nem teste getters apenas para inflar cobertura.
- Meça com JaCoCo e um gate Maven que falhe abaixo dos limites quando configurado.
  Os POMs atuais ainda não configuram JaCoCo nem esse gate. Sem relatório,
  registre cobertura como não verificada; nunca declare aprovação por suposição.

## FIRST

| Princípio | Aplicação |
|---|---|
| Fast | Testar transições sem iniciar Spring. |
| Independent | Criar dados próprios, sem depender da ordem ou de outro teste. |
| Repeatable | Usar `Clock` fixo, dados determinísticos e infraestrutura isolada. |
| Self-validating | Verificar resultados com asserções, sem inspeção manual de logs. |
| Timely | Escrever testes antes da implementação, no ciclo TDD. |

## Estrutura e ferramentas

Use JUnit Jupiter, AssertJ e Mockito do `spring-boot-starter-test`. Use Mockito
para colaboradores unitários; não simule semântica específica de banco ou broker
quando ela for o objeto da validação. Use Spring Test/MockMvc na fronteira HTTP,
contextos Spring quando o wiring fizer parte do comportamento, `spring-rabbit-test`
para suporte AMQP e Testcontainers para integrações reais. Declare dependências
de teste ausentes quando implementar os cenários que precisarem delas.

Siga Given/When/Then ou AAA (Arrange/Act/Assert), separando etapas com linhas em
branco, sem comentários redundantes. Exemplo unitário ilustrativo:

```java
@Test
void rejectsJobOwnedByAnotherUser() {
    var repository = mock(JobRepository.class);
    var query = new FindJob(repository);
    var jobId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    when(repository.findByIdAndUserId(jobId, ownerId))
        .thenReturn(Optional.empty());

    var failure = catchThrowable(() -> query.execute(jobId, ownerId));

    assertThat(failure).isInstanceOf(JobNotFoundException.class);
}
```

Evite `testsAllJobOperations`, testes sem asserções, `Thread.sleep`, rede externa
ou mocks que apenas repetem cada chamada interna da implementação.

## Cenários obrigatórios por mudança

| Mudança | Validação |
|---|---|
| Controller | Autenticação, propriedade, validação, status, headers e JSON. |
| Segurança | Anônimo, proprietário autorizado e acesso entre usuários. |
| Ciclo de vida | Cada transição adicionada, inválidas e eventos antigos. |
| Repository/migration | PostgreSQL via Testcontainers, constraints e rollback. |
| Publisher/listener | Sucesso, retry, redelivery, duplicatas e mensagens malformadas. |
| Outbox | Rollback conjunto, confirms e publicadores concorrentes. |

Na iteração, execute os testes mais específicos. Ao concluir o módulo, rode
`./mvnw -pl services/video-api -am verify` na raiz; alterações entre módulos exigem
`./mvnw verify`. Confirme que testes de integração foram realmente executados
pela configuração Maven. Informe comandos, resultados e impedimentos reais.
