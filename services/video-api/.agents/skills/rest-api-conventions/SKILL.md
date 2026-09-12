---
name: rest-api-conventions
description: Complementar a skill video-api-http com critérios genéricos de paginação e consistência REST. Não define paths, envelopes ou schemas para o Video API.
---

# REST API Conventions

Consulte `video-api-http` e `contracts/openapi.yaml` antes desta skill. O
contrato existente determina paths, versões, status, headers, IDs, schemas de
sucesso e formatos de erro.

- Preserve o contrato público ao adicionar ou alterar endpoints; não migre rotas
  não relacionadas.
- Use DTOs de borda e não serialize entidades JPA.
- Para coleções paginadas, defina tamanho máximo, ordenação permitida e critério
  estável de desempate no contrato antes da implementação.
- Não serialize `PageImpl` diretamente como contrato público.
- Use um handler consistente para falhas; o formato deve ser o definido ou
  documentado no OpenAPI, não um envelope introduzido por conveniência.
- Uma resposta `204` não possui corpo.
