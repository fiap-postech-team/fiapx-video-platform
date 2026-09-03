# ADR 0006 — Usar object storage compatível com S3

- **Status:** Aceito
- **Data:** 2026-08-30

## Contexto

Vídeos e ZIPs podem ser grandes, têm ciclo de vida próprio e não devem consumir banco relacional ou memória do broker. A
solução local precisa ser executável sem depender de uma nuvem específica, mantendo portabilidade para produção.

## Alternativas consideradas

1. **API S3 com MinIO local:** separa binários do domínio e oferece portabilidade, mas introduz consistência eventual e
   políticas de bucket.
2. **Filesystem compartilhado:** simples numa única máquina, porém dificulta escala horizontal, durabilidade e deploy
   multi-host.
3. **BYTEA/large objects no PostgreSQL:** transação unificada, ao custo de backups maiores, I/O competitivo e baixa
   eficiência operacional.
4. **Payload no RabbitMQ:** rejeitado por tamanho, memória, throughput e retenção.

## Decisão

Armazenar entradas e resultados em object storage compatível com S3. Usar MinIO no desenvolvimento local e permitir
serviços S3-compatible nos demais ambientes. Mensagens e banco guardam apenas object keys e metadados. Arquivos
temporários do processor são efêmeros e removidos ao final.

## Consequências positivas

- escala e retenção de binários independentes do banco;
- transferência eficiente e possibilidade de URLs pré-assinadas;
- processor stateless entre mensagens.

## Consequências negativas e riscos

- objetos órfãos são possíveis entre falhas de storage e banco;
- segurança depende de bucket policies, criptografia e URLs com expiração;
- compatibilidade S3 não elimina diferenças entre fornecedores.

## Mitigações e revisão

Definir prefixos por tenant/job, lifecycle policies, checksums, limites de tamanho, criptografia e reconciliação de
órfãos. Criar testes de contrato contra MinIO e o provedor escolhido antes de produção.
