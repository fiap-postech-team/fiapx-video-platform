# Regras para Java 21

## 1. Prefira recursos modernos do Java 21

Use recursos que deixem o modelo de domínio explícito e reduzam código repetitivo.

**Evite**

```java
public final class VideoStatus {
    private final String value;

    public VideoStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```

**Prefira**

```java
public record VideoResponse(UUID id, VideoStatus status, Instant createdAt) {}

public sealed interface VideoStatus permits Pending, Processing, Completed, Failed {}

public record Pending() implements VideoStatus {}
public record Processing(int progress) implements VideoStatus {}
public record Completed(URI outputUrl) implements VideoStatus {}
public record Failed(String reason) implements VideoStatus {}

public String toLabel(VideoStatus status) {
    return switch (status) {
        case Pending ignored -> "PENDING";
        case Processing ignored -> "PROCESSING";
        case Completed ignored -> "COMPLETED";
        case Failed ignored -> "FAILED";
    };
}
```

Use tipos de `java.time` (`Instant`, `Duration`, `LocalDate`) no lugar das APIs legadas de data. Use `Optional` apenas em retornos nos quais a ausência seja esperada, nunca em campos, DTOs, parâmetros ou coleções.

Recomendação prática: use `record` para DTOs e objetos de valor imutáveis, `sealed` para estados fechados de domínio e expressões `switch` para tratar todos os estados.

## 2. Concorrência e performance

Use concorrência somente quando ela melhorar vazão ou latência, mantendo o uso de recursos limitado.

**Evite**

```java
ExecutorService executor = Executors.newCachedThreadPool();
executor.submit(() -> processVideo(video));
```

**Prefira**

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> notificationClient.send(notification));
}
```

Para trabalho limitado por CPU, use um pool limitado de threads de plataforma dimensionado pelos processadores disponíveis.

```java
ExecutorService cpuExecutor = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()
);
```

Recomendação prática: use virtual threads para I/O bloqueante em alto volume, pools limitados para trabalho de CPU e sempre defina timeout, limite de fila e política de rejeição.

## 3. Não bloqueie fluxos críticos com operações pesadas

O tratamento HTTP deve validar entrada, persistir o estado necessário e responder rapidamente. Não transcodifique mídia, gere miniaturas ou chame serviços externos lentos no caminho síncrono da requisição.

**Evite**

```java
@PostMapping("/videos")
public ResponseEntity<VideoResponse> upload(@RequestBody UploadRequest request) {
    var output = transcoder.transcode(request.sourceUrl());
    return ResponseEntity.ok(output);
}
```

**Prefira**

```java
@PostMapping("/videos")
public ResponseEntity<VideoResponse> upload(@Valid @RequestBody UploadRequest request) {
    VideoResponse response = videoService.createProcessingJob(request);

    return ResponseEntity.accepted()
        .location(URI.create("/videos/" + response.id()))
        .body(response);
}
```

Use workers assíncronos, filas e outbox transacional para tarefas intensivas de I/O ou longa duração.

Recomendação prática: classifique o trabalho antes de implementá-lo. Trabalho de CPU precisa de execução limitada; trabalho de I/O deve ser assíncrono, idempotente, com retentativas e prazo de execução.

## 4. Configure por ambiente

Mantenha a configuração fora da aplicação e valide os valores obrigatórios na inicialização.

**Evite**

```java
String databaseUrl = "jdbc:postgresql://production-db:5432/videos";
String apiKey = "secret-value";
```

**Prefira**

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}

video:
  storage:
    bucket: ${VIDEO_STORAGE_BUCKET}
```

```java
@ConfigurationProperties(prefix = "video.storage")
@Validated
public record StorageProperties(@NotBlank String bucket) {}
```

Use perfis Spring apenas para comportamento específico de ambiente. Mantenha `.env` local fora do versionamento e disponibilize um `.env.example` sanitizado.

Recomendação prática: falhe rapidamente quando faltar configuração obrigatória, nunca defina segredos de produção como padrão e documente todas as variáveis de ambiente necessárias.

## 5. Implemente encerramento gracioso

Permita que requisições e mensagens em processamento terminem com segurança antes de parar a aplicação.

**Prefira**

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

```java
@Component
public class WorkerShutdownListener {

    @PreDestroy
    void stopAcceptingNewWork() {
        // Interrompe novos consumos e aguarda os jobs ativos no prazo configurado.
    }
}
```

Garanta que workers parem de receber mensagens antes do fechamento de conexões de banco, clientes HTTP ou executores.

Recomendação prática: configure um prazo de desligamento, torne jobs idempotentes e assegure que processamento interrompido possa ser repetido com segurança após reinício.

## 6. Use logging centralizado e estruturado

Use SLF4J como API de logs e Logback como implementação. Registre eventos relevantes em formato estruturado e pesquisável.

**Evite**

```java
System.out.println("Processando vídeo " + videoId);
```

**Prefira**

```java
private static final Logger log = LoggerFactory.getLogger(VideoProcessor.class);

log.info("video_processing_started videoId={} traceId={}", videoId, traceId);
```

Use MDC para incluir identificadores de correlação de forma consistente.

```java
try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", traceId)) {
    log.info("video_processing_completed videoId={}", videoId);
}
```

Recomendação prática: inclua `traceId`, identificadores de agregados e eventos, e nome da operação. Use INFO para marcos de negócio e ERROR apenas para falhas acionáveis.

## 7. Proteja segredos e dados sensíveis

Nunca registre credenciais, cabeçalhos de autorização, URLs assinadas, dados pessoais ou payloads completos com campos sensíveis.

**Evite**

```java
log.info("Requisição de upload: {}", request);
log.debug("Cabeçalho de autorização: {}", authorizationHeader);
```

**Prefira**

```java
log.info(
    "upload_requested videoId={} contentType={} contentLength={}",
    videoId,
    request.contentType(),
    request.contentLength()
);
```

Use gerenciadores de segredos ou variáveis de ambiente para credenciais. Mascare valores sensíveis em mensagens de exceção e sanitizadores de log.

Recomendação prática: trate logs como dados operacionais acessíveis externamente; registre identificadores e metadados, nunca segredos ou conteúdo bruto de usuários.

## 8. Mantenha o build reprodutível

Use Maven Wrapper e fixe versões de dependências e plugins pelo gerenciamento de dependências.

**Prefira**

```bash
./mvnw verify
```

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.release>21</maven.compiler.release>
</properties>
```

Use `maven-enforcer-plugin` para exigir versões suportadas de Java e Maven. Use `dependency:tree` para inspecionar dependências transitivas e evitar bibliotecas duplicadas ou conflitantes.

Recomendação prática: execute o mesmo comando Maven Wrapper localmente e na CI, evite intervalos de versão e revise mudanças de dependência quanto a segurança e licença.

## 9. Mantenha dependências direcionais

Regras de negócio não devem depender de controllers HTTP, entidades JPA ou infraestrutura de mensageria. As dependências devem apontar para domínio e casos de uso.

**Evite**

```java
@Service
public class VideoService {
    private final VideoController controller;
}
```

**Prefira**

```java
public interface VideoRepository {
    Optional<Video> findById(UUID id);
    Video save(Video video);
}

@Service
public class CreateVideoJobUseCase {
    private final VideoRepository videoRepository;

    public CreateVideoJobUseCase(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }
}
```

Recomendação prática: defina interfaces nas fronteiras da aplicação, injete dependências pelo construtor e corrija dependências circulares em vez de habilitá-las.

## 10. Padronize erros e respostas HTTP

Use tratamento centralizado de exceções e um contrato estável de respostas de erro.

**Prefira**

```java
public record ApiError(String code, String message, String traceId, Instant timestamp) {}
```

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(VideoNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(
        VideoNotFoundException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError(
                "VIDEO_NOT_FOUND",
                "Vídeo não encontrado.",
                request.getHeader("X-Trace-Id"),
                Instant.now()
            ));
    }
}
```

Não exponha stack traces, erros SQL ou detalhes internos a clientes da API.

Recomendação prática: mapeie exceções de domínio para status HTTP explícitos, mantenha códigos de erro estáveis e teste cada contrato público de erro.

## 11. Valide entrada na fronteira

Valide DTOs de requisição antes de executar regras de negócio. Use Jakarta Validation e constraints customizadas para regras específicas de domínio.

**Prefira**

```java
public record CreateVideoRequest(
    @NotBlank @Size(max = 255) String fileName,
    @NotNull URI sourceUrl,
    @NotBlank String contentType
) {}
```

```java
@PostMapping("/videos")
ResponseEntity<VideoResponse> create(@Valid @RequestBody CreateVideoRequest request) {
    return ResponseEntity.accepted().body(useCase.execute(request));
}
```

Recomendação prática: valide formato e limites na borda HTTP, invariantes de negócio no domínio e retorne erros por campo de forma consistente.

## 12. Trate persistência e transações com cuidado

Mantenha transações curtas, defina suas fronteiras em serviços de aplicação e evite carregar dados desnecessários.

**Evite**

```java
@Transactional
public void process(VideoEntity video) {
    externalClient.upload(video.getContent());
    video.setStatus("COMPLETED");
}
```

**Prefira**

```java
@Transactional
public void markCompleted(UUID videoId) {
    VideoEntity video = repository.findById(videoId)
        .orElseThrow(() -> new VideoNotFoundException(videoId));

    video.markCompleted();
    outboxRepository.save(VideoCompletedEvent.from(video));
}
```

Use paginação em coleções, índices em colunas usadas com frequência para filtro ou junção, e projeções quando a entidade completa não for necessária. Evite N+1 com estratégias de busca explícitas.

Recomendação prática: não mantenha transações de banco durante chamadas remotas; persista estado e eventos de outbox atomicamente e publique de forma assíncrona.

## 13. Teste comportamento de negócio primeiro

Use testes unitários focados para regras de domínio e testes de integração quando validar banco, mensageria ou comportamento do framework HTTP.

**Prefira**

```java
@Test
void shouldRejectCompletionForAlreadyFailedVideo() {
    Video video = Video.failed(UUID.randomUUID(), "Mídia inválida");

    assertThatThrownBy(video::markCompleted)
        .isInstanceOf(InvalidVideoStateException.class);
}
```

Use Testcontainers quando testes dependerem de contratos reais de PostgreSQL, RabbitMQ ou outra infraestrutura.

Recomendação prática: teste fluxos de sucesso e erro relevante para cada mudança de comportamento, mantenha testes determinísticos e execute o módulo Maven mais específico durante o desenvolvimento.

## 14. Inclua observabilidade no serviço

Exponha health checks, métricas e tracing distribuído desde o início.

**Prefira**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
```

```java
Counter.builder("video.processing.completed")
    .description("Quantidade de jobs de processamento concluídos")
    .register(meterRegistry)
    .increment();
```

Monitore latência, taxa de erros, profundidade de filas, duração de processamento, retentativas e saturação de recursos. Propague contexto de trace por requisições HTTP e mensagens.

Recomendação prática: defina alertas a partir de indicadores de serviço, use readiness checks para dependências necessárias ao tráfego e preserve `traceId` em todos os fluxos assíncronos.
