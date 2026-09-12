# Padrões de código

## Limites e legibilidade

- Arquivos novos ou alterados de código, inclusive testes: máximo de 100 linhas,
  contando imports, linhas em branco e declarações. Não comprima instruções para
  contornar o limite. Migrations já aplicadas continuam imutáveis.
- Métodos e construtores: máximo de 30 linhas, da assinatura à chave final.
- Não ultrapasse três níveis de `if/else` aninhados. Prefira cláusulas de guarda
  e early return; não troque aninhamento por ternários ou streams ilegíveis.
- Use nomes de negócio, tipos específicos e dependências imutáveis. Evite `Object`,
  tipos raw, booleanos de modo e números mágicos.

Exemplo de arquivo excessivo: não concentre consulta, criação e publicação num
`JobService.java` de 250 linhas. Separe responsabilidades em `FindJob.java`,
`CreateJob.java` e `OutboxPublisher.java`, cada um até 100 linhas. Não crie classes
sem responsabilidade própria apenas para repartir linhas.

Exemplo de método excessivo: não escreva `create()` com 45 linhas de validação,
montagem e persistência. Extraia operações nomeadas, mantendo job e outbox na
mesma transação do caso de uso.

## Comentários

Não insira comentários, salvo quando absolutamente necessários para explicar uma
restrição que o código não consegue expressar. Prefira renomear e simplificar.

Evite:
```java
// Busca o job pelo identificador.
var j = repository.findById(id);
```

Prefira:
```java
var ownedJob = repository.findByIdAndUserId(jobId, ownerId);
```

Uma expressão regular complexa inevitável pode justificar explicação:
```java
// Impede segmentos '..' completos sem rejeitar pontos dentro do nome.
private static final Pattern PARENT_SEGMENT =
    Pattern.compile("(?:^|/)\\.\\.(?:/|$)");
```

Esse exemplo verifica apenas segmentos, não é validação completa de uma chave S3.

## Condicionais e cláusulas de guarda

Evite acumular condições e esconder a regra no nível mais interno:
```java
if (event != null) {
    if (!alreadyHandled(event.id())) {
        if (job.accepts(event.status())) {
            apply(event);
        }
    }
}
```

Prefira entradas inválidas explícitas e retornos antecipados:
```java
Objects.requireNonNull(event, "event");
if (alreadyHandled(event.id())) {
    return;
}
if (!job.accepts(event.status())) {
    throw new InvalidJobTransitionException();
}
apply(event);
```

A deduplicação desse exemplo precisa de garantia transacional durável; verificar
um conjunto em memória antes de `apply` não resolve concorrência.

## Code smells e Clean Code

| Não fazer | Fazer |
|---|---|
| `process(job, true, false)` | Casos de uso nomeados, como `completeJob`. |
| `if (retryCount > 7)` espalhado | Política de retry configurável e centralizada. |
| Copiar validação de transição em várias camadas | Uma regra de domínio. |
| `catch (Exception ignored) {}` | Tratar falha específica ou propagar ao handler. |
| `return null` para qualquer falha | Ausência explícita ou exceção de domínio. |
| Controller decide estado e publica RabbitMQ | Controller delega ao caso de uso. |
| `new RabbitTemplate()` na regra de negócio | Dependência injetada na borda. |

Não adicione abstrações para necessidades hipotéticas. Aplique também
[SOLID](architecture.md), preservando coesão e transações.
