# ADR 0010 — Autorização por proprietário e papel administrativo somente leitura

- **Status:** Aceito
- **Data:** 2026-09-15

## Contexto

A consulta de job buscava somente por ID, permitindo que qualquer identidade autenticada lesse jobs de terceiros.
Object keys não podem funcionar como mecanismo de autorização. O MVP precisa de dois papéis — USER e ADMIN — com
acesso administrativo estritamente operacional e somente leitura, sem expor existência ou conteúdo de recursos de
outros proprietários.

## Alternativas consideradas

1. **Filtro de proprietário no caso de uso e na consulta ao banco:** o par (id, userId) é resolvido em uma única
   query; recursos de terceiros são indistinguíveis de inexistentes (404).
2. **Filtro em memória após a consulta:** vaza existência por timing/contagem e escala mal; rejeitada.
3. **Autorização por object key ou por identidade informada em payload/query:** contorna o JWT e é falsificável;
   rejeitada. A identidade atuante é sempre o `sub` do token validado.
4. **Papéis configuráveis ou por tenant:** além do escopo do MVP; rejeitada.

## Decisão

- Authorities derivam somente da claim assinada `roles` (array de strings), mapeada para `ROLE_USER`/`ROLE_ADMIN`;
  valores ausentes, com tipo inesperado ou desconhecidos não geram authority.
- Rotas `/v1/admin/**` exigem `ROLE_ADMIN` e oferecem somente leitura operacional de jobs (listagem com filtros por
  proprietário/status e consulta por id). Não há endpoint administrativo de mutação, reprocessamento, troca de
  proprietário, operação de filas/DLQ ou concessão de papéis.
- Toda consulta ou mutação de usuário combina ID do recurso e proprietário no banco; acesso a recurso de terceiro
  retorna 404 idêntico ao de recurso inexistente.
- Erros seguem RFC 7807 (ProblemDetail): 401 credencial ausente/inválida, 403 sem papel, 404 inexistente ou de
  outro proprietário, 409 operação incompatível com o estado.
- Acesso administrativo gera log sanitizado (administrador, operação, tipo do recurso, horário UTC, resultado), sem
  payloads, object keys, URLs assinadas ou dados pessoais.

### Decisões vinculantes para features futuras

- O endpoint de download, quando existir, permanecerá restrito ao proprietário **inclusive para ADMIN**.
- O cadastro público, quando existir, não aceitará papéis; a concessão de ADMIN jamais ocorrerá por endpoint
  público.

## Consequências positivas

- isolamento completo entre proprietários, sem oráculo de existência;
- superfície administrativa pequena, auditável e sem mutações privilegiadas;
- contrato de erros uniforme para clientes.

## Consequências negativas e riscos

- tokens continuam emitidos externamente com HMAC compartilhado nesta fase;
- um token ADMIN vazado expõe metadados operacionais de todos os jobs (mitigado por auditoria e pela ausência de
  acesso a conteúdo);
- listagem do usuário não é paginada nesta iteração.

## Mitigações e revisão

Migrar para OIDC/JWKS com chaves assimétricas e rotação de segredos antes de exposição real; revisar a auditoria
quando novos tipos de recurso administrável surgirem; paginar a listagem do usuário se o volume crescer.
