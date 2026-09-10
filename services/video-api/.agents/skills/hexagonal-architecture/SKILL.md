---
name: hexagonal-architecture
description: Complementar a arquitetura do Video API com princípios de portas e adaptadores. Não impõe uma árvore global de pacotes nem substitui organização por capacidade.
---

# Hexagonal Architecture

Consulte `architecture.md` antes desta skill. Organize código por capacidade do
Video API; portas e adaptadores são uma forma de preservar as fronteiras, não
uma exigência de diretórios globais `domain`, `application` e `infrastructure`.

- Mantenha decisões de domínio independentes de Spring, JPA, HTTP e broker.
- Defina portas somente onde houver fronteira externa ou variação útil.
- Faça casos de uso dependerem de abstrações de persistência, mensageria ou
  serviços externos, e mantenha adaptadores responsáveis pelo framework.
- Mantenha transações na camada de aplicação e comportamento no domínio.
- Não introduza interfaces, mapeadores ou camadas apenas para reproduzir um
  diagrama arquitetural.
