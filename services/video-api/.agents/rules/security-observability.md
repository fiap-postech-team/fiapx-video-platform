# Segurança e observabilidade

- Nunca versione credenciais, tokens, chaves privadas, URLs assinadas ou conexões
  de produção. Use variáveis de ambiente; defaults locais ficam no desenvolvimento.
- Inicialização em produção deve falhar com segredo obrigatório ausente ou
  placeholder inseguro.
- Exponha probes de saúde apenas conforme necessário ao runtime; proteja demais
  endpoints Actuator conforme o modelo de implantação.
- Não registre JWTs, credenciais, URLs assinadas, dados pessoais brutos ou payloads
  completos que possam conter informações sensíveis.
- Propague correlação do HTTP ao job, à outbox e aos eventos de resultado.
- Emita métricas de criação, transições, backlog e falhas da outbox, falhas do
  consumidor e latência de processamento.
- Não use jobId, userId ou correlationId como labels de métricas: causam alta
  cardinalidade. Use correlação em logs sanitizados.

## Exemplos

Evite registrar o evento completo:
```java
log.info("Received event: {}", event);
```

Prefira apenas metadados permitidos pela política de logs:
```java
log.info("Result received eventId={} correlationId={}",
    event.id(), event.correlationId());
```

Evite configuração de produção que substitui segredo ausente por `changeme`.
Prefira validação que interrompe inicialização com mensagem sanitizada, sem
imprimir o valor recebido.

Teste configuração inválida, acesso aos endpoints protegidos e ausência de dados
sensíveis nos logs quando essas áreas forem alteradas. Preserve diagnóstico útil
sem capturar exceções silenciosamente.
