# ADR 0001 — Usar Java 21 e Spring Boot 3

- **Status:** Aceito
- **Data:** 2026-08-30
- **Responsáveis:** Equipe FIAP X

## Contexto

As três aplicações precisam de uma base consistente para HTTP, segurança, persistência, mensageria, configuração,
métricas e testes. O runtime deve ser estável, ter suporte amplo no ecossistema e funcionar bem em containers. A
premissa do projeto exige Java 21 e Spring Boot 3.x.

## Forças de decisão

- suporte a longo prazo do Java 21;
- disponibilidade de Spring Security, Data JPA, AMQP, Actuator e Testcontainers;
- produtividade e familiaridade esperada da equipe;
- imagens de runtime maduras e portabilidade entre ambientes.

## Alternativas consideradas

1. **Java 21 com Spring Boot 3:** integração consistente e grande ecossistema, com custo de memória e startup maior que
   stacks nativas.
2. **Quarkus ou Micronaut:** menor consumo e startup rápido, mas acrescentaria curva de aprendizado e divergiria da
   premissa.
3. **Implementação sem framework:** máximo controle, porém custo desproporcional para segurança, configuração,
   observabilidade e integrações.

## Decisão

Usar Java 21 e a linha estável 3.5.x do Spring Boot. Versões serão centralizadas no POM raiz e atualizadas de forma
coordenada, após `clean verify` e validação das imagens.

## Consequências positivas

- configuração uniforme e menor quantidade de código de infraestrutura;
- acesso a virtual threads e demais recursos modernos do Java quando houver caso de uso;
- manutenção e contratação facilitadas por tecnologias difundidas.

## Consequências negativas e riscos

- baseline mínimo de Java 21 em todas as máquinas e imagens;
- atualizações de patch podem alterar dependências transitivas;
- footprint superior a alternativas compiladas nativamente.

## Mitigações e revisão

Fixar versões, usar Maven Wrapper, executar testes e análise de dependências no CI. Rever se metas mensuráveis de
memória, startup ou custo não forem atendidas; considerar CDS/AOT antes de trocar o framework.
