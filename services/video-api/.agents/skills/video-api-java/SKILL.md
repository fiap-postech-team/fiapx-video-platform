---
name: video-api-java
description: Aplicar práticas de Java 21, concorrência, configuração, shutdown, logging e build ao código Java em services/video-api. Não substitui skills de HTTP, persistência, testes, lifecycle ou observabilidade.
---

# Regras para Java 21

Use esta skill para decisões de linguagem e runtime Java em `services/video-api`.
Para limites e legibilidade, consulte [Padrões de código](../../rules/code-standards.md);
para fronteiras, HTTP, persistência, testes e observabilidade, consulte as skills específicas.

## Recursos modernos do Java 21

Use `record` para DTOs e objetos de valor imutáveis, `sealed` para estados
fechados de domínio e expressões `switch` exaustivas para todos os estados.
Use `Instant`, `Duration` e `LocalDate` em vez das APIs legadas. Use `Optional`
somente em retornos cuja ausência seja esperada, nunca em campos, DTOs,
parâmetros ou coleções.

## Concorrência e performance

Use concorrência somente quando melhorar vazão ou latência, com uso de recursos
limitado. Use virtual threads para I/O bloqueante em alto volume e pools limitados
de threads de plataforma para trabalho de CPU. Defina timeout, limite de fila e
política de rejeição.

Não execute transcodificação, geração de miniaturas ou chamadas remotas lentas no
caminho síncrono HTTP. Para classificação do trabalho, outbox e retentativas,
consulte [persistência e mensageria](../video-api-persistence-messaging/SKILL.md)
e [event-driven messaging](../event-driven-messaging/SKILL.md).

## Configuração e shutdown

Mantenha configuração fora da aplicação, valide valores obrigatórios na
inicialização e nunca defina segredos de produção como padrão. Use perfis Spring
somente para comportamento específico de ambiente.

Configure shutdown gracioso com prazo explícito. Workers devem parar de aceitar
novas mensagens antes de conexões e executores fecharem; processamento interrompido
deve ser seguro para repetição.

## Logging e build

Use SLF4J e logging estruturado. Inclua `traceId`, identificadores de agregados ou
eventos e a operação; use INFO para marcos de negócio e ERROR apenas para falhas
acionáveis. Nunca registre segredos, tokens, URLs assinadas, dados pessoais ou
payloads sensíveis; a política completa está em
[Segurança e observabilidade](../../rules/security-observability.md).

Use Maven Wrapper e versões fixadas pelo gerenciamento de dependências. Execute o
mesmo comando na máquina local e CI, evite intervalos de versão e revise mudanças
de dependência quanto a segurança e licença.
