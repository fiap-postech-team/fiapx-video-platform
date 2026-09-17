# ADR 0011 — Identidade local com JWT RSA e fronteira para OIDC

- **Status:** Aceito
- **Data:** 2026-09-16

## Contexto

O MVP precisa cadastrar usuários e manter sessões sem um provedor OIDC disponível. O ADR 0010 assumia que o
serviço só validaria tokens e restringia HMAC ao desenvolvimento local. Essa responsabilidade mudou: o `video-api`
passa a emitir tokens locais e precisa preservar uma troca futura de provedor sem expor chaves ou claims aos casos
de uso.

## Forças de decisão

- access tokens precisam permanecer bearer JWT verificáveis no Resource Server;
- logout e reuso de refresh token devem invalidar a sessão imediatamente;
- segredos e configurações inseguras não podem existir fora de `local`;
- a migração futura para OIDC/JWKS não pode exigir reescrever regras de cadastro ou sessão.

## Alternativas consideradas

1. HMAC compartilhado em todos os ambientes: simples, mas mistura emissão e validação por uma única chave secreta.
2. OIDC obrigatório agora: correto operacionalmente, mas fora do escopo e sem emissor disponível.
3. JWT RSA local com portas de emissão e resolução de identidade: separa chave privada/pública e mantém a fronteira de migração.

## Decisão

O MVP emitirá JWT `RS256` localmente, com chave privada e pública externas fora de `local`. Cada token contém
`sub`, `iss`, `aud`, `iat`, `exp`, `jti`, `sid` e `roles`; o Resource Server valida assinatura, issuer, audience,
expiração e sessão ativa. Refresh tokens são opacos, rotativos, armazenados somente como hash e revogam sua cadeia
quando reutilizados. Casos de uso dependem de portas para persistência, emissão e resolução de identidade.

O cadastro público cria apenas `USER`. O primeiro `ADMIN` só pode ser criado por bootstrap explicitamente habilitado
com segredo externo. O ADR 0010 é substituído, mas mantido como registro da decisão anterior.

## Consequências positivas

- chaves de emissão e validação são separadas;
- logout e reuso invalidam imediatamente os JWTs vinculados à sessão;
- controllers não dependem da estrutura interna do JWT;
- a integração futura OIDC troca adaptadores, não o núcleo de identidade.

## Consequências negativas e riscos

- validar a sessão adiciona uma consulta PostgreSQL a cada bearer request;
- sessões e histórico de refresh tokens exigem retenção e limpeza operacional;
- o serviço temporariamente acumula responsabilidades de Resource Server e emissor.

## Mitigações

Validar configurações obrigatórias na inicialização, limitar access tokens a quinze minutos, usar cookies HttpOnly
SameSite Strict e CSRF para operações por cookie. Remover sessões expiradas/revogadas por rotina operacional futura.

## Critérios de revisão

Revisar quando um provedor OIDC/JWKS for adotado, quando houver requisito de clientes cross-site ou quando o custo da
resolução de sessão exigir cache com invalidação confiável.
