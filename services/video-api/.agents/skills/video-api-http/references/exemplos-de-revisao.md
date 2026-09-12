# Exemplos de revisão

| Não fazer | Fazer |
|---|---|
| `findById(jobId)` no acesso do usuário | `findByIdAndUserId(jobId, authenticatedOwnerId)`. |
| `request.userId()` define proprietário | Identidade do contexto de segurança. |
| `ResponseEntity<JobEntity>` | DTO conforme o schema público de Job. |
| Capturar toda exceção e retornar `200` | Mapear falhas específicas no handler global. |
| Retornar stack trace ou SQL no erro | Detalhe público sanitizado. |
