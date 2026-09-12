---
name: testing-pyramid
description: Complementar a skill video-api-testing com padrões de testes unitários, slice e integração em Spring Boot. Não substitui cenários, cobertura ou contratos definidos pelo Video API.
---

# Testing Pyramid

Use esta skill somente depois de consultar `video-api-testing`. Se o contrato, a
autorização ou o comportamento do Video API definirem uma escolha diferente,
eles prevalecem.

- Prefira testes unitários sem contexto Spring para regras de negócio.
- Use `@WebMvcTest` quando a fronteira HTTP fizer parte do comportamento.
- Use `@DataJpaTest` e Testcontainers quando semântica de PostgreSQL importar.
- Use `@SpringBootTest` apenas quando o comportamento exigir integração de
  componentes; não trate uma distribuição percentual de tipos de teste como meta.
- Use AssertJ e testes determinísticos. `@Mock` com `MockitoExtension` e
  `Mockito.mock()` são ambos aceitáveis quando mantêm o teste claro e isolado.
- Para endpoints, derive rota, status, headers e JSON de `contracts/openapi.yaml`;
  não copie exemplos de outros recursos ou envelopes.

Teste cenários de sucesso e falhas relevantes no nível mais específico que
comprove o comportamento.
