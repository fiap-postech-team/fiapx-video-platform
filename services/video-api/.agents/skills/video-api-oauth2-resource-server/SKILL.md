---
name: video-api-oauth2-resource-server
description: Configurar ou revisar validação de bearer JWT pelo OAuth2 Resource Server do Video API. Não use para emitir tokens, implementar login, refresh token ou compartilhar chaves de assinatura.
---

# OAuth2 Resource Server

Use esta skill ao alterar autenticação bearer, mapeamento de identidade ou
autorização no Video API. O serviço valida tokens recebidos; não emite JWTs nem
mantém endpoints de login ou refresh token.

- Preserve `spring-boot-starter-oauth2-resource-server`; não introduza JJWT ou
  filtros próprios de assinatura sem decisão arquitetural explícita.
- Configure issuer URI ou JWK Set URI por variável de ambiente segura. Valide
  issuer, audience quando aplicável, expiração e assinatura pelo Resource Server.
- Extraia a identidade atuante do `JwtAuthenticationToken` ou `Authentication`
  validado; valide o subject conforme o contrato local e nunca aceite `userId`
  recebido no payload como identidade.
- Mapeie authorities somente a partir de claims confiáveis e proteja endpoints
  Actuator não públicos conforme o modelo de implantação.
- Retorne 401 para credenciais ausentes ou inválidas em rotas protegidas e 403
  para identidade autenticada sem permissão. Mantenha o formato de erro alinhado
  à [skill de HTTP e autorização](../video-api-http/SKILL.md) e ao OpenAPI.
- Nunca registre o bearer token, claims sensíveis ou cabeçalhos de autorização.

Teste token ausente, inválido, expirado, subject malformado, proprietário válido,
acesso entre usuários e authorities quando a regra de acesso mudar.
