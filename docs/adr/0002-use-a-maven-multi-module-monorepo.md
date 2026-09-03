# ADR 0002 — Usar monorepo Maven multi-module

- **Status:** Aceito
- **Data:** 2026-08-30

## Contexto

API, processor e worker têm deploy e escala independentes, mas compartilham ciclo inicial de desenvolvimento, padrões de
qualidade e contratos. É necessário equilibrar autonomia dos serviços com uma experiência simples para a equipe e para a
avaliação do projeto.

## Forças de decisão

- revisão atômica de mudanças que atravessam contratos;
- uma única entrada para build e CI;
- centralização de versões sem compartilhar domínio ou persistência;
- tamanho inicial reduzido da equipe.

## Alternativas consideradas

1. **Monorepo Maven multi-module:** coordenação simples e refactors visíveis, com risco de pipeline e ownership
   acoplados.
2. **Um repositório por serviço:** maior isolamento e permissões granulares, ao custo de sincronização de contratos e
   configuração duplicada.
3. **Aplicação única modular:** operação mais simples no início, mas não permite escala e falha independentes para
   FFmpeg e notificações.

## Decisão

Manter os três serviços em um monorepo com parent POM apenas para versionamento e plugins. Nenhum serviço terá
dependência Maven direta de outro. Contratos HTTP e assíncronos ficam em diretório neutro e versionado.

## Consequências positivas

- checkout, onboarding e atualização de dependências simplificados;
- mudanças de schema e consumidores podem ser revisadas na mesma pull request;
- políticas de CI e documentação ficam descobríveis em um único lugar.

## Consequências negativas e riscos

- um build completo pode crescer com o número de módulos;
- ownership e releases podem parecer acoplados mesmo sem dependência técnica;
- permissões por diretório são menos fortes que por repositório.

## Mitigações e revisão

Manter builds isoláveis com `-pl`, Dockerfiles próprios e CODEOWNERS quando necessário. Considerar extração se equipes,
permissões, cadências de release ou tempo de CI divergirem de forma sustentada.
