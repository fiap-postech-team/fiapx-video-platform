# ADR 0010 — Usar HMAC JWT apenas no desenvolvimento local e migrar para OIDC/JWKS fora dele

- **Status:** Aceito
- **Data:** 2026-09-15

## Contexto

O `video-api` desta fundação não implementa autenticação nem emite tokens, mas a documentação e os épicos futuros precisam deixar clara a estratégia de evolução. O ambiente local de desenvolvimento pode usar um segredo compartilhado simples para depuração e testes manuais, enquanto ambientes não locais exigem um provedor de identidade compatível com OIDC/JWKS.

## Alternativas consideradas

1. **Não definir estratégia agora:** mantém o escopo menor, mas deixa a próxima etapa sem direção documentada.
2. **HMAC JWT em todos os ambientes:** simples no início, porém fraco para rotação, compartilhamento e operação fora do desenvolvimento.
3. **OIDC/JWKS em todos os ambientes desde já:** mais correto para produção, mas adiciona complexidade antes de existir a integração funcional.
4. **HMAC apenas local + OIDC/JWKS fora de local:** separa a conveniência de desenvolvimento da política real de ambientes compartilhados.

## Decisão

O `video-api` não emitirá tokens nesta fundação. Quando a autenticação local for necessária para desenvolvimento, ela usará HMAC apenas com o perfil `local` e permanecerá restrita ao ambiente local. Em qualquer ambiente não local, a autenticação deve ser baseada em OIDC/JWKS; segredos compartilhados e fallback inseguros são proibidos.

## Consequências positivas

- desenvolvimento local continua simples;
- a política de produção já nasce separada da conveniência local;
- a migração futura para OIDC/JWKS fica explícita e sem reescrita de ADR anterior.

## Consequências negativas e riscos

- dois modos de autenticação precisarão ser mantidos na evolução futura;
- HMAC local pode ser confundido com estratégia de produção se a documentação ficar ambígua;
- o épico de autenticação ainda precisará implementar a integração real quando houver casos de uso.

## Mitigações

Limitar HMAC ao perfil `local`, registrar a decisão na documentação de arquitetura e manter o `video-api` sem emissão de token nesta fundação. Em ambientes não locais, exigir OIDC/JWKS e tratar ausência de identidade como falha de configuração.

## Critérios de revisão

Reavaliar apenas quando o primeiro fluxo autenticado for implementado. Se o modo local passar a ser compartilhado além de desenvolvimento, ou se o ambiente alvo mudar, criar novo ADR em vez de alterar este histórico.
