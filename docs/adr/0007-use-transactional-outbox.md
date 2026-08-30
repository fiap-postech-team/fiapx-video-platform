# ADR 0007 — Usar transactional outbox

- **Status:** Aceito
- **Data:** 2026-08-30

## Contexto

Ao criar um job, a API precisa persistir o estado e solicitar processamento. Uma gravação no PostgreSQL seguida de publicação no RabbitMQ apresenta uma janela de falha: o job pode existir sem mensagem. Publicar primeiro cria a situação inversa. Transações distribuídas entre os dois sistemas não são desejáveis.

## Alternativas consideradas

1. **Transactional outbox com polling:** simples e transacional no banco, com atraso pequeno e necessidade de limpeza.
2. **Publicação direta e compensação:** menos tabelas, mas possui janela de perda difícil de provar e recuperar.
3. **XA/2PC:** atomicidade forte, porém maior complexidade, acoplamento e suporte operacional limitado.
4. **CDC/Debezium:** outbox robusta e desacoplada, mas acrescenta infraestrutura prematura para esta fundação.

## Decisão

Persistir job e registro de outbox na mesma transação local. Um publisher agendado lê eventos não publicados, envia ao exchange e registra `publishedAt`. O payload usa o mesmo contrato versionado do broker.

## Consequências positivas

- nenhuma criação confirmada depende de publicação síncrona no broker;
- eventos pendentes ficam observáveis e recuperáveis no banco;
- não há transação distribuída.

## Consequências negativas e riscos

- envio e marcação não são atômicos, portanto duplicatas são esperadas;
- múltiplas réplicas podem disputar os mesmos registros;
- polling introduz latência e crescimento da tabela.

## Mitigações e revisão

Implementar claim com `FOR UPDATE SKIP LOCKED`, publisher confirms, contador de tentativas, índice parcial, retenção e métrica de idade do evento mais antigo. Avaliar CDC quando volume de polling ou requisitos de latência justificarem a complexidade.
