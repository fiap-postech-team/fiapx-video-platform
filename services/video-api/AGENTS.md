# Instruções do Video API

Aplicam-se a todo `services/video-api` e complementam o
[AGENTS.md da raiz](../../AGENTS.md). Leia todas as regras vinculadas antes de
implementar ou revisar mudanças. Elas são obrigatórias, não apenas referências.

## Regras específicas

- [Arquitetura e SOLID](.agents/rules/architecture.md): responsabilidades e dependências.
- [Padrões de código](.agents/rules/code-standards.md): limites, comentários, condicionais e Clean Code.
- [Boas práticas de Java 21](.agents/rules/java.md): linguagem, concorrência, configuração, logging e observabilidade.
- [Testes e cobertura](.agents/rules/testing-coverage.md): TDD, FIRST, AAA e mínimo de 80%.
- [HTTP e autorização](.agents/rules/http-api.md): JWT, propriedade, DTOs e contratos.
- [Ciclo de vida](.agents/rules/job-lifecycle.md): transições, histórico e idempotência.
- [Persistência e mensageria](.agents/rules/persistence-messaging.md): migrations, outbox e concorrência.
- [Segurança e observabilidade](.agents/rules/security-observability.md): configuração, logs e métricas.

## Aplicação e revisão

Preserve Java 21, Spring Boot e Maven. As fontes de verdade são
[OpenAPI](../../contracts/openapi.yaml), [AsyncAPI](../../contracts/asyncapi.yaml)
e os [ADRs](../../docs/adr/README.md).

Os exemplos são ilustrativos: nomes de classes e métodos não significam que a
implementação já existe. Não modifique contratos apenas para copiar exemplos.
Aponte violações com arquivo, linha, impacto e severidade. Falhas de autorização,
ausência de testes de comportamento, perda de atomicidade ou de idempotência
impedem aprovação. Não declare verificações não executadas nem trate regra
documentada como ferramenta já configurada.
