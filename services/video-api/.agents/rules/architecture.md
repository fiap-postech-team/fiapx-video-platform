# Arquitetura e SOLID

O Video API valida identidade, recebe jobs, consulta e persiste seu estado,
publica trabalho via outbox e consome resultados. Processar mídia pertence ao
`video-processor`; entregar notificações pertence ao worker de notificações.

- Organize pacotes por capacidade, como `jobs` e `outbox`, evitando diretórios
  globais `controller`, `service` e `repository`.
- Mantenha decisões de domínio independentes de HTTP, JPA e RabbitMQ.
- Use injeção por construtor e campos `final`, sem field injection.
- Use UUID para IDs de agregados/eventos e UTC para timestamps; injete `Clock`
  quando o tempo participar de uma regra testável.
- Não acrescente dependência de produção se a plataforma atual atende ao caso.
  Justifique necessidade e custo quando uma adição for indispensável.
- Mudanças arquiteturais relevantes exigem novo ADR, sem reescrever ADR aceito.

## Exemplos SOLID

| Princípio | Não fazer | Fazer |
|---|---|---|
| SRP: responsabilidade única | Controller decide transição e grava outbox. | Controller adapta HTTP; caso de uso coordena; domínio decide transições. |
| OCP: aberto à extensão | Copiar condicionais de tipos de evento em várias camadas. | Encapsular tratamento por tipo quando houver variantes reais. |
| LSP: substituição | Implementação de consulta retorna jobs de outro usuário. | Toda implementação mantém o contrato de consulta por proprietário. |
| ISP: interfaces específicas | Consulta depende de interface com publicar e notificar. | Depender somente das operações de leitura necessárias. |
| DIP: inversão de dependência | Regra de transição importa `RabbitTemplate`. | Domínio decide estado; adaptadores cuidam do banco e broker. |

Exemplo ilustrativo de dependência explícita:
```java
final class FindJob {
    private final JobReader jobs;

    FindJob(JobReader jobs) {
        this.jobs = jobs;
    }

    JobView execute(UUID jobId, UUID ownerId) {
        return jobs.findOwned(jobId, ownerId)
            .orElseThrow(JobNotFoundException::new);
    }
}
```

Introduza interfaces para fronteiras e variações úteis, não uma interface por
classe automaticamente. DTOs públicos não devem expor entidades JPA.
