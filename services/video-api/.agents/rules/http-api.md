# HTTP e autorização

- Controllers tratam autenticação, validação, mapeamento de DTOs, status e headers;
  delegam regras de negócio a casos de uso.
- Extraia identidade do JWT validado. Nunca aceite `userId` do cliente como
  identidade atuante. Valide o formato do subject conforme o contrato local.
- Toda consulta ou mutação exige ID do job e proprietário autenticado, salvo
  endpoint privilegiado explicitamente documentado.
- Valide payloads com Bean Validation. Use DTOs independentes do modelo JPA.
- Preserve status, headers e schemas do OpenAPI. A criação atual responde `201`;
  o processamento continua assíncrono. Exponha localização estável do recurso e
  documente novos headers no contrato.
- Centralize erros num handler consistente, preferencialmente `ProblemDetail`.
  O OpenAPI atual ainda não define schemas de erro: documente-os junto da
  implementação, sem inventar formatos diferentes em cada controller.

## Exemplos de revisão

| Não fazer | Fazer |
|---|---|
| `findById(jobId)` no acesso do usuário | `findByIdAndUserId(jobId, authenticatedOwnerId)`. |
| `request.userId()` define proprietário | Identidade do contexto de segurança. |
| `ResponseEntity<JobEntity>` | DTO conforme o schema público de Job. |
| Capturar toda exceção e retornar `200` | Mapear falhas específicas no handler global. |
| Retornar stack trace ou SQL no erro | Detalhe público sanitizado. |

Não remova campos públicos silenciosamente. `sourceKey` consta no contrato atual;
uma representação mais restrita exige alinhamento com OpenAPI e compatibilidade
com clientes. Não exponha chaves internas extras sem necessidade contratual.

Teste autenticação, acesso entre usuários, validação e contrato conforme
[testes e cobertura](testing-coverage.md).
