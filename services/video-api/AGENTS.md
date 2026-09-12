# Instruções do Video API

Aplicam-se a todo `services/video-api` e complementam o
[AGENTS.md da raiz](../../AGENTS.md). Leia todas as regras vinculadas antes de
implementar ou revisar mudanças. Elas são obrigatórias, não apenas referências.

## Regras específicas

- [Arquitetura e SOLID](.agents/rules/architecture.md): responsabilidades e dependências.
- [Padrões de código](.agents/rules/code-standards.md): limites, comentários, condicionais e Clean Code.
- [Skill de boas práticas de Java 21](.agents/skills/video-api-java/SKILL.md): linguagem, concorrência, configuração, logging e observabilidade.
- [Skill de testes e cobertura](.agents/skills/video-api-testing/SKILL.md): TDD, FIRST, AAA e mínimo de 80%.
- [Skill de HTTP e autorização](.agents/skills/video-api-http/SKILL.md): JWT, propriedade, DTOs e contratos.
- [Skill de OAuth2 Resource Server](.agents/skills/video-api-oauth2-resource-server/SKILL.md): validação de bearer token e mapeamento de identidade.
- [Skill de ciclo de vida dos jobs](.agents/skills/video-api-job-lifecycle/SKILL.md): transições, histórico e idempotência.
- [Skill de persistência e mensageria](.agents/skills/video-api-persistence-messaging/SKILL.md): migrations, outbox e concorrência.
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
